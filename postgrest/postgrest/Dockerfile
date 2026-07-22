# Pin an explicit version instead of floating `latest`, so a rebuild months
# from now can't silently pull in breaking changes.
FROM postgrest/postgrest:v12.2.11

# Runs as this non-root user by default (baked into the upstream image);
# kept explicit here so it survives if the base image ever changes it.
# No shell/package manager in this image (it's a static binary on a
# distroless-style base) — config is passed entirely via PGRST_* env vars
# from docker-compose instead of a baked-in config file, which also keeps
# secrets (JWT secret, DB password) out of the image layers.
USER 1000
