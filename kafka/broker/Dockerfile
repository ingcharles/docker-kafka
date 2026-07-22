# Pin an explicit version instead of floating `latest`, so a rebuild months
# from now can't silently pull in breaking changes.
FROM apache/kafka:3.9.0

# Runs as this non-root user by default (baked into the upstream image);
# kept explicit here so it survives if the base image ever changes it.
USER appuser
