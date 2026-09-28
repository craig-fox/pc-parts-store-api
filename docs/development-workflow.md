# Development and Release Workflow

## Branches

The project uses two long-lived branches:

| Branch    | Purpose                                                                                            |
| --------- | -------------------------------------------------------------------------------------------------- |
| `develop` | Default branch for local development, integration, and pre-release validation.                     |
| `main`    | Production branch. Changes merged here represent approved releases intended for deployment to AWS. |

Feature and fix branches should be created from `develop` and merged back through pull requests.

## Development workflow

1. Update your local `develop` branch:

   ```bash
   git switch develop
   git pull
   ```
2. Create a short-lived feature or fix branch:

   ```bash
   git switch -c feature/short-description
   ```
3. Implement the change and run the relevant tests locally.
4. Push the branch and open a pull request targeting `develop`.
5. Ensure the required GitHub Actions checks pass and obtain the required review.
6. Merge the pull request into `develop`.

Do not commit directly to `develop` or `main`.

## Continuous integration

Pull requests targeting `develop` run the configured GitHub Actions workflow. The pipeline validates the build and tests, runs the configured end-to-end checks, and builds the Docker images as defined in `.github/workflows/build.yml`.

A pull request must satisfy the repository's required status checks and review rules before it can be merged.

## Release workflow

1. Confirm that `develop` is stable and all required CI checks pass.
2. Create a release pull request from `develop` into `main`.
3. Use a release title, such as `Release 1.3.0`, and document the changes included.
4. Review the release diff, verify the checks and required approvals, and confirm that the release is ready.
5. Merge the release pull request into `main`.
6. Deploy the approved release to AWS using the project's release and deployment process.
7. Tag the released commit with the corresponding version, for example `v1.3.0`, and publish release notes.

## Hotfixes

For an urgent production fix, create a short-lived hotfix branch from `main`, apply and test the fix, and open a pull request targeting `main`. After the fix is merged and released, merge or cherry-pick the fix back into `develop` so the branches remain aligned.

## Branch protection

Both `develop` and `main` are protected by GitHub rulesets. Contributors should use pull requests rather than pushing directly to these branches. Required reviews and status checks are governed by the repository's configured rulesets.

## Local branch setup

After cloning the repository, fetch the remote branches and set up local tracking:

```bash
git fetch origin
git switch --track -c develop origin/develop
```

To update your local development branch:

```bash
git switch develop
git pull
```
