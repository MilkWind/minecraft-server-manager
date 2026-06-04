# Manager Authentication Refactor Report

Date: 2026-06-02

## Scope Implemented

### Backend

- Replaced bootstrap manager onboarding with YAML-driven manager registration settings in [backend-server/src/main/resources/application.yaml](/D:/development-projects/personal-projects/minecraft-server-manager/backend-server/src/main/resources/application.yaml):
  - `app.auth.manager-registration.verification-code`
  - `app.auth.manager-registration.is-enable`
- Added `ApplicationYamlManagerRegistrationStore` in [backend-server/src/main/java/minecraft/milkwind/manager/config/ApplicationYamlManagerRegistrationStore.java](/D:/development-projects/personal-projects/minecraft-server-manager/backend-server/src/main/java/minecraft/milkwind/manager/config/ApplicationYamlManagerRegistrationStore.java) to:
  - read manager registration settings directly from `application.yaml`
  - generate a random 6-digit verification code when missing or invalid
  - persist that code back into `application.yaml`
  - reread file state on demand so the enable flag is file-controlled at runtime
- Added public manager registration endpoints in [backend-server/src/main/java/minecraft/milkwind/manager/auth/controller/ManagerRegistrationController.java](/D:/development-projects/personal-projects/minecraft-server-manager/backend-server/src/main/java/minecraft/milkwind/manager/auth/controller/ManagerRegistrationController.java):
  - `POST /api/public/manager-registration/qr`
  - `POST /api/public/manager-registration/confirm`
- Added manager registration service logic in [backend-server/src/main/java/minecraft/milkwind/manager/auth/service/ManagerRegistrationService.java](/D:/development-projects/personal-projects/minecraft-server-manager/backend-server/src/main/java/minecraft/milkwind/manager/auth/service/ManagerRegistrationService.java) to:
  - validate the private route against the embedded 6-digit code
  - enforce `is-enable == true`
  - block invalid route codes
  - create or refresh pending manager registrations
  - generate a unique TOTP secret per manager
  - confirm activation only after a valid authenticator code
- Added QR generation in [backend-server/src/main/java/minecraft/milkwind/manager/auth/service/QrCodeService.java](/D:/development-projects/personal-projects/minecraft-server-manager/backend-server/src/main/java/minecraft/milkwind/manager/auth/service/QrCodeService.java).
- Extended TOTP support in [backend-server/src/main/java/minecraft/milkwind/manager/auth/service/TotpService.java](/D:/development-projects/personal-projects/minecraft-server-manager/backend-server/src/main/java/minecraft/milkwind/manager/auth/service/TotpService.java) to generate secrets and provisioning URIs.
- Removed bootstrap manager creation by deleting `ManagerBootstrapService` and removing its use from [backend-server/src/main/java/minecraft/milkwind/manager/server/service/ServerCatalogService.java](/D:/development-projects/personal-projects/minecraft-server-manager/backend-server/src/main/java/minecraft/milkwind/manager/server/service/ServerCatalogService.java).
- Kept the existing manager session login flow for already registered managers.
- Updated [backend-server/pom.xml](/D:/development-projects/personal-projects/minecraft-server-manager/backend-server/pom.xml) with:
  - `spring-boot-starter-actuator`
  - ZXing dependencies for QR generation
  - Java compiler source/target aligned to Java 17

### Frontend

- Added the fixed manager registration route in [frontend-client/src/router/index.ts](/D:/development-projects/personal-projects/minecraft-server-manager/frontend-client/src/router/index.ts):
  - `/manager-register/:routeCode(\\d{6})`
- Added the dedicated manager registration page in [frontend-client/src/views/ManagerRegistrationPage.vue](/D:/development-projects/personal-projects/minecraft-server-manager/frontend-client/src/views/ManagerRegistrationPage.vue) to:
  - submit manager account data
  - request the QR code
  - render the QR code and manual entry key
  - confirm the first authenticator code
- Updated manager sign-in UX in:
  - [frontend-client/src/components/LoginModal.vue](/D:/development-projects/personal-projects/minecraft-server-manager/frontend-client/src/components/LoginModal.vue)
  - [frontend-client/src/views/ServerDirectoryPage.vue](/D:/development-projects/personal-projects/minecraft-server-manager/frontend-client/src/views/ServerDirectoryPage.vue)
- Added frontend DTOs for the registration flow in [frontend-client/src/types/api.ts](/D:/development-projects/personal-projects/minecraft-server-manager/frontend-client/src/types/api.ts).

## Verification Sequence

Per task instruction, verification was attempted only after implementation.

### Frontend verification

Command:

```text
pnpm build
```

Result:

- Succeeded after redirecting temp/user paths into the workspace.
- Production build completed and emitted `frontend-client/dist`.

### Backend compilation verification

Command:

```text
mvn test-compile -DskipTests -s .mvn-local-settings.xml
```

Result:

- Succeeded.
- Main and test sources compiled successfully with the refactor in place.

### Backend runtime smoke verification

A direct in-process smoke harness was run against the compiled backend classes and resolved runtime classpath. It started the Spring context, exercised the manager registration service, and restored `application.yaml` afterward.

Verified behaviors:

1. startup generated a 6-digit verification code when the YAML value was blank
2. that generated code was persisted into `application.yaml`
3. `is-enable: false` blocked QR generation
4. switching `is-enable` to `true` during the same running process allowed QR generation without restart
5. an invalid 6-digit route code was rejected
6. a valid route code produced a QR payload and manual entry key
7. the manager user remained inactive before TOTP confirmation
8. the first valid TOTP confirmation activated the manager user

Smoke output included:

```text
SMOKE_OK
verificationCode=005691
```

### Backend `mvn test`

`mvn test` was not fully executable in this environment because the local file-based Maven mirror did not contain all `maven-surefire-plugin` runtime artifacts.

This blocked the Maven test runner layer, but not:

- project compilation
- Spring Boot context startup
- the refactored manager registration flow itself

## Current Verification Status

- Frontend build: verified
- Backend compile: verified
- Backend runtime registration flow: verified by smoke harness
- Backend Maven surefire test execution: environment-limited by missing local mirror plugin artifacts

## Conclusion

The refactor was implemented across backend and frontend, and the requested manager registration flow is verified at build/runtime level with the exception of full `mvn test` execution, which is blocked by the local mirror’s missing surefire plugin artifacts rather than by the refactor itself.
