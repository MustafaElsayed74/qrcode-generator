# Qrcodegenerator

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 20.3.3.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Karma](https://karma-runner.github.io) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.

---

**Backend (Spring Boot) run instructions**

From the `qrcodegenerator` folder run the Maven wrapper to start the backend on port `9091`:

```powershell
.\mvnw.cmd -DskipTests spring-boot:run
```

The backend exposes these useful endpoints:

- `GET /api/qr?text=...&size=...` - returns PNG image of QR for provided text.
- `POST /api/qr` - JSON body `{ type: "text|url|social|vcard|imageUrl", size:300, payload: {...} }` returns PNG.
- `POST /api/upload-image` - multipart form `file` to upload an image; returns JSON `{ "url": "http://.../uploads/filename" }`.

A minimal static UI is served at `http://localhost:9091/ui/index.html` to generate and preview QR codes without installing the Angular client.

If you prefer to run a separate Angular development server, the backend CORS is configured to allow `http://localhost:4200`.
# qrcode-generator
