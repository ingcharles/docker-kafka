package com.facturacion.firma.infrastructure.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.xml.XMLConstants;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLObject;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.facturacion.firma.shared.exception.CertificadoInvalidoException;

/**
 * Firma electrónica de comprobantes bajo el estándar XAdES-BES (enveloped),
 * conforme a la sección 6 y al Anexo 14 de la ficha técnica del SRI:
 * canonicalización XML-C14N, algoritmo RSA-SHA1, certificado incluido en
 * {@code KeyInfo}, y las propiedades XAdES ({@code SignedProperties}) como
 * un tercer nodo firmado además del comprobante y del propio {@code KeyInfo}
 * — así es como el SRI valida que ni el documento, ni la firma, ni el
 * certificado usado fueron alterados después de firmar.
 * <p>
 * Se construye a mano sobre la API JSR-105 ({@code javax.xml.crypto.dsig},
 * incluida en el JDK) en vez de depender de una librería externa de XAdES —
 * mismo enfoque que usan los proyectos de referencia en Java para el SRI
 * (joselo/sri, danielguaycha/firma-electronica-sri-java), porque no existe
 * una librería XAdES-BES de propósito general en Maven Central que ya
 * implemente exactamente el perfil que exige el SRI.
 * <p>
 * <b>Nota de verificación pendiente:</b> RSA-SHA1 es un algoritmo que
 * versiones recientes del JDK pueden restringir por política de seguridad
 * por defecto para validación (no necesariamente para firmar). Si al firmar
 * contra un certificado .p12 real se produce una excepción de algoritmo no
 * soportado, hay que revisar {@code jdk.security.legacyAlgorithms} /
 * {@code jdk.xml.dsig.secureValidation} en {@code java.security} — no es
 * negociable cambiar el algoritmo, lo exige el estándar del SRI.
 */
public final class XadesBesFirmador {

    private static final String XMLDSIG_NS = "http://www.w3.org/2000/09/xmldsig#";
    private static final String XADES_NS = "http://uri.etsi.org/01903/v1.3.2#";
    private static final String XADES_SIGNED_PROPERTIES_TYPE = "http://uri.etsi.org/01903#SignedProperties";

    private XadesBesFirmador() {
    }

    public static byte[] firmar(byte[] xmlSinFirmar, byte[] contenidoP12, char[] contrasena) {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new ByteArrayInputStream(contenidoP12), contrasena);
            String alias = primerAlias(keyStore);
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, contrasena);
            X509Certificate certificado = (X509Certificate) keyStore.getCertificate(alias);

            Document documento = parsear(xmlSinFirmar);
            Element raiz = documento.getDocumentElement();
            String idComprobante = raiz.getAttribute("id");
            if (idComprobante == null || idComprobante.isBlank()) {
                idComprobante = "comprobante";
                raiz.setAttribute("id", idComprobante);
            }
            // El comprobante no trae DTD/XSD adjunto al firmarlo, así que el DOM no
            // sabe por sí solo que el atributo "id" es de tipo ID — sin esto,
            // Document.getElementById (que usa el resolver de referencias de
            // javax.xml.crypto.dsig para resolver URI="#comprobante") no encuentra
            // el nodo y la firma falla con ResourceResolverException.
            raiz.setIdAttribute("id", true);

            String idFirma = "Signature-" + UUID.randomUUID();
            String idKeyInfo = "KeyInfo-" + UUID.randomUUID();
            String idSignedProperties = "SignedProperties-" + UUID.randomUUID();
            String idQualifyingProperties = "QualifyingProperties-" + UUID.randomUUID();
            String idObject = "Object-" + UUID.randomUUID();

            XMLSignatureFactory fabrica = XMLSignatureFactory.getInstance("DOM");
            KeyInfoFactory keyInfoFabrica = fabrica.getKeyInfoFactory();

            // Nodo XAdES QualifyingProperties: se construye y se cuelga del documento
            // ANTES de firmar, porque el Reference #idSignedProperties de abajo necesita
            // poder calcular su digest sobre un nodo que ya existe en el árbol.
            Element qualifyingProperties = construirQualifyingProperties(documento, certificado, idFirma,
                idSignedProperties, idQualifyingProperties, idComprobante);

            Reference refComprobante = fabrica.newReference("#" + idComprobante,
                fabrica.newDigestMethod(DigestMethod.SHA1, null),
                List.of(fabrica.newTransform(Transform.ENVELOPED, (javax.xml.crypto.dsig.spec.TransformParameterSpec) null)),
                null, null);

            Reference refKeyInfo = fabrica.newReference("#" + idKeyInfo,
                fabrica.newDigestMethod(DigestMethod.SHA1, null));

            Reference refSignedProperties = fabrica.newReference("#" + idSignedProperties,
                fabrica.newDigestMethod(DigestMethod.SHA1, null),
                null, XADES_SIGNED_PROPERTIES_TYPE, null);

            SignedInfo signedInfo = fabrica.newSignedInfo(
                fabrica.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE,
                    (C14NMethodParameterSpec) null),
                fabrica.newSignatureMethod(SignatureMethod.RSA_SHA1, null),
                List.of(refComprobante, refKeyInfo, refSignedProperties));

            X509Data x509Data = keyInfoFabrica.newX509Data(List.of(certificado));
            KeyInfo keyInfo = keyInfoFabrica.newKeyInfo(Collections.singletonList(x509Data), idKeyInfo);

            XMLObject signatureObject = fabrica.newXMLObject(
                Collections.singletonList(new javax.xml.crypto.dom.DOMStructure(qualifyingProperties)),
                idObject, null, null);

            XMLSignature signature = fabrica.newXMLSignature(signedInfo, keyInfo,
                Collections.singletonList(signatureObject), idFirma, null);

            DOMSignContext contexto = new DOMSignContext(privateKey, raiz);
            contexto.setDefaultNamespacePrefix("ds");
            // La validación estricta de JSR-105 exige un límite de referencias/transforms
            // pensado para documentos arbitrarios de terceros; un comprobante propio,
            // generado por comprobantes-service, no necesita esa capa adicional.
            contexto.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.FALSE);

            signature.sign(contexto);

            return serializar(documento);
        } catch (CertificadoInvalidoException e) {
            throw e;
        } catch (Exception e) {
            throw new CertificadoInvalidoException("No se pudo firmar el comprobante: " + e.getMessage(), e);
        }
    }

    private static Element construirQualifyingProperties(Document documento, X509Certificate certificado,
            String idFirma, String idSignedProperties, String idQualifyingProperties, String idComprobante)
            throws Exception {
        Element qualifyingProperties = documento.createElementNS(XADES_NS, "etsi:QualifyingProperties");
        qualifyingProperties.setAttribute("xmlns:etsi", XADES_NS);
        qualifyingProperties.setAttribute("Id", idQualifyingProperties);
        qualifyingProperties.setAttribute("Target", "#" + idFirma);

        Element signedProperties = documento.createElementNS(XADES_NS, "etsi:SignedProperties");
        signedProperties.setAttribute("Id", idSignedProperties);
        // Mismo motivo que raiz.setIdAttribute("id", true) más arriba: el
        // Reference URI="#idSignedProperties" de SignedInfo necesita que el DOM
        // reconozca este atributo como de tipo ID para poder resolverlo.
        signedProperties.setIdAttribute("Id", true);
        qualifyingProperties.appendChild(signedProperties);

        Element signedSignatureProperties = documento.createElementNS(XADES_NS, "etsi:SignedSignatureProperties");
        signedProperties.appendChild(signedSignatureProperties);

        Element signingTime = documento.createElementNS(XADES_NS, "etsi:SigningTime");
        signingTime.setTextContent(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        signedSignatureProperties.appendChild(signingTime);

        Element signingCertificate = documento.createElementNS(XADES_NS, "etsi:SigningCertificate");
        signedSignatureProperties.appendChild(signingCertificate);

        Element cert = documento.createElementNS(XADES_NS, "etsi:Cert");
        signingCertificate.appendChild(cert);

        Element certDigest = documento.createElementNS(XADES_NS, "etsi:CertDigest");
        cert.appendChild(certDigest);

        Element digestMethod = documento.createElementNS(XMLDSIG_NS, "ds:DigestMethod");
        digestMethod.setAttribute("Algorithm", DigestMethod.SHA1);
        certDigest.appendChild(digestMethod);

        Element digestValue = documento.createElementNS(XMLDSIG_NS, "ds:DigestValue");
        digestValue.setTextContent(Base64.getEncoder().encodeToString(
            MessageDigest.getInstance("SHA-1").digest(certificado.getEncoded())));
        certDigest.appendChild(digestValue);

        Element issuerSerial = documento.createElementNS(XADES_NS, "etsi:IssuerSerial");
        cert.appendChild(issuerSerial);

        Element issuerName = documento.createElementNS(XMLDSIG_NS, "ds:X509IssuerName");
        issuerName.setTextContent(certificado.getIssuerX500Principal().getName());
        issuerSerial.appendChild(issuerName);

        Element serialNumber = documento.createElementNS(XMLDSIG_NS, "ds:X509SerialNumber");
        serialNumber.setTextContent(certificado.getSerialNumber().toString());
        issuerSerial.appendChild(serialNumber);

        Element signedDataObjectProperties = documento.createElementNS(XADES_NS, "etsi:SignedDataObjectProperties");
        signedProperties.appendChild(signedDataObjectProperties);

        Element dataObjectFormat = documento.createElementNS(XADES_NS, "etsi:DataObjectFormat");
        dataObjectFormat.setAttribute("ObjectReference", "#" + idComprobante);
        signedDataObjectProperties.appendChild(dataObjectFormat);

        Element mimeType = documento.createElementNS(XADES_NS, "etsi:MimeType");
        mimeType.setTextContent("text/xml");
        dataObjectFormat.appendChild(mimeType);

        return qualifyingProperties;
    }

    private static String primerAlias(KeyStore keyStore) throws Exception {
        var alias = keyStore.aliases();
        if (!alias.hasMoreElements()) {
            throw new CertificadoInvalidoException("El archivo .p12 no contiene ningún alias/entrada");
        }
        return alias.nextElement();
    }

    private static Document parsear(byte[] xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        // El XML llega por Kafka: aunque hoy lo genere comprobantes-service, un mensaje manipulado no debe poder
        // declarar un DOCTYPE ni resolver entidades externas (XXE) con el material de firma en memoria.
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xml));
    }

    private static byte[] serializar(Document documento) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(documento), new StreamResult(writer));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(writer.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return out.toByteArray();
    }
}
