# Java JSON Schema Generator – Slate Docs

## Locally building documentation (with Docker)

### Prerequisite
- a Docker installation, e.g. [Docker Desktop](https://www.docker.com/products/docker-desktop)

### Steps to build (and edit) documentation
1. Switch to `slate-docs` directory
    ```sh
    cd ./slate-docs
    ```
2. Start Docker container
    ```sh
    docker run -d --rm --name slate -p 4567:4567 -v $(pwd)/build:/srv/slate/build -v $(pwd)/source:/srv/slate/source slate
    ```
3. Build Slate Docs via the Docker container
    ```sh
    docker exec -it slate /bin/bash -c "bundle exec middleman build"
    ```
4. Open/refresh `slate-docs/build/index.html` in your browser to see the latest build results
5. Optionally: make changes in `slate-docs/source/index-html.md` and/or `slate-docs/source/includes/*.md` files.
6. Repeat steps 3-5 as needed.
7. Shutdown Docker container:
    ```sh
    docker stop slate
    ```

## Publishing of documentation changes
The generation and deployment are automated through GitHub Actions:
- On every pull request build, the documentation is generated to ensure no major errors were introduced.
- On every release being created in GitHub, the documentation is generated and deployed to https://victools.github.io/jsonschema-generator
