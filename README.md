# wildfly-github-bot
wildfly-github-bot helps you to keep your pull requests in the correct format.

This project is build with usage of Quarkus GitHub App: https://quarkiverse.github.io/quarkiverse-docs/quarkus-github-app/dev/index.html

## Development
### Step 1 - Register the application

After forking the project we need to register wildfly-github-bot on GitHub for your account. Go to [GitHub Apps](https://github.com/settings/apps) and click on `New GitHub Apps`

Also, you can access this page by clicking on your profile picture on gitHub and go to `Settings > Developer Settings > GitHub Apps > New GitHub App`.

Fields to fill in:

1. GitHub App name- should be unique
2. Homepage URL- the link to the forked project
3. Webhook URL
- Go to [smee.io](https://smee.io/) and press `Start a new channel`
- Add this link to `Webhook URL`
4. Set up permissions
- `Commit statuses` - `Access: Read and write`
- `Contents` - `Access Read-only`
- `Metadata` - `Access: Read-only`
- `Pull requests` - `Access: Read and write`
5. Subscribe to events
- `Push` 
- `Pull requests`
- `Pull request review`
- `Pull request review comment`
6. Create a private key

   After creating the app scroll down and press `Generate a private key`

   You will be asked to download the key, keep it around, we will need it for the next step.

### Step 2 - Set up the app

As the configuration is environment-specific, and you probably don’t want to commit it in your repository, the best is to create in the root a `.env` file.

The content of your .env file should be as follows:

```
QUARKUS_GITHUB_APP_APP_ID=<the numeric app id>
QUARKUS_GITHUB_APP_APP_NAME=<the name of your app>
QUARKUS_GITHUB_APP_WEBHOOK_PROXY_URL=<your Smee.io channel URL>
QUARKUS_GITHUB_APP_PRIVATE_KEY=-----BEGIN RSA PRIVATE KEY-----\
                 <your private key>                          \
-----END RSA PRIVATE KEY-----

QUARKUS_MAILER_FROM=<email address>
QUARKUS_MAILER_USERNAME=<email address>
QUARKUS_MAILER_PASSWORD=<email password>
```

> **_NOTE:_**  If you do not wish to send emails, do not fill in the `QUARKUS_MAILER_*` variables please.

**QUARKUS_GITHUB_APP_APP_ID**
The numeric app id appears in the App ID field.

**QUARKUS_GITHUB_APP_APP_NAME**
The name of your app is the one appearing in the GitHub URL. It is only used to improve usability in dev mode.

**QUARKUS_GITHUB_APP_WEBHOOK_PROXY_URL**
The URL you obtained when you created your Smee.io channel.

**QUARKUS_GITHUB_APP_PRIVATE_KEY**
The content of the private key you generated and downloaded. Open the key file with a text editor as key viewers usually only show fingerprints.

**QUARKUS_MAILER_FROM**
email address displayed in the message

**QUARKUS_MAILER_USERNAME**
email address sending the email

**QUARKUS_MAILER_PASSWORD**
password to the email address corresponding to QUARKUS_MAILER_USERNAME. **Note** You probably want to generate it using _Gmail_ > _Settings_ > _Security_ > _2-Step Verification_ > _App passwords_

***Default email service***
is Gmail. To change this behavior or to override predefined parameters in _applications.properties_ file please refer to [Mailer Extension Documentation](https://quarkus.io/guides/mailer-reference#popular)
### Step 3 - Set up the app

1. Create a new repo or use an already created one in which you want to track PRs.
2. Go to the settings of your GitHub App and go to `Install App > Install > Only select repositories > Select the one you need > Install`
3. In your repo in the main branch create a folder `.github` and a file `wildfly-bot.yml` with xml code in it:
```
wildfly:
 rules:
   - title: "test"
   - body: "test"
     notify: [xstefank,petrberan]
 format:
   title:
     message: "Wrong content of the PR title"
   description:
     regexes:
       - pattern: "JIRA:\\s+https://issues.redhat.com/browse/WFLY-\\d+|https://issues.redhat.com/browse/WFLY-\\d+"
         message: "The PR description must contain a link to the JIRA issue"
 emails:
   - foo@bar.baz
   - user@acme.org
```

1. `title`- Checks the title of a PR by using a regular expression generated from `projectKey` field, which is by default "WFLY". You can find more information in [wildfly-bot-config-example.yml](wildfly-bot-config-example.yml)
2. `description`- Checks comments of a PR by using individual regular expressions in the `pattern` fields under `regexes`.
> The correct format in example is "https://issues.jboss.org/browse/WFLY-11"
4. `message` - The text of an error message in the respective check.
5. `emails` - List of emails to receive notifications.

> **_NOTE:_**  `title` and `commit` are enabled by default. More [here](wildfly-bot-config-example.yml).

Also, there is a possibility to select checks that you need. Just left in the `wildfly-bot.yml` file checks you need.

Like this:
```
wildfly:
 rules:
   - title: "test"
   - body: "test"
     notify: [xstefank,petrberan]
 format:
   title:
     message: "Wrong content of the PR title"
 emails:
   - nonexisistingemail@whatever.com
   - whoever@nonexistingmailingservice.com
```

### Run the application in dev mode

Run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Dev UI available in dev mode only at http://localhost:8080/q/dev/.
> > **_NOTE:_**  In Dev mode sending emails is mocked. To disable this, set the following property `quarkus.mailer.mock=false`

Try to create a PR and update it a few times. The format check sends commit statuses that you will see in the PR.

## Deployment on OpenShift

### Requirements

 - JDK 17+ with **JAVA_HOME** configured appropriately
 - OpenShift (e.g, https://developers.redhat.com/developer-sandbox)
 - [OpenShift CLI](https://docs.openshift.com/container-platform/4.7/cli_reference/openshift_cli/getting-started-cli.html)
 - Apache Maven 3.8.6

### Step 1 - Register new GitHub App

Fill in the following information.
1. Application name
2. Homepage URL
3. Webhook URL
   - put any placeholder URL here, as you will get the URL after the deployment
4. Webhook secret
   - You can generate a secret by using GitHub's recommended method:
     > ruby -rsecurerandom -e 'puts SecureRandom.hex(20)'
   - or use `pwgen`:
     > pwgen -N 1 -s 40
   - save it as you will need it later
5. Permissions
   - `Commit statuses` - `Access: Read and write`
   - `Metadata` - `Access: Read-only`
   - `Pull requests` - `Access: Read and write`
6. Subscribe to events
   - `Pull requests`
   - `Pull request review comment`

### Step 2 - Generate a private key

 - Scroll down to generate a private key
 - Download it as you will need it later

### Step 3 - Install the application in the desired repository

 - You can find this in the "Install App" tab of your GitHub application

### Step 4 - Log into the OpenShift cluster
 - `oc login -u <username>`
   - You will need to fill required information in prompt
 - `oc login --token=<token> --server=<serverUrl>`
   - You can request the token via the `Copy Login Command` link in the OpenShift web console.

### Step 5 - Create OpenShift secret with webhook secret and private key

      `oc create secret generic wildfly-bot --from-literal=QUARKUS_GITHUB_APP_WEBHOOK_SECRET=<your-webhook-secret> --from-file=QUARKUS_GITHUB_APP_PRIVATE_KEY=<path-to-your-private-key>`

> **_NOTE:_**  If you wish to use mailing option, please append the following properties to the previous command ` --from-literal=QUARKUS_MAILER_FROM=<email address> --from-literal=QUARKUS_MAILER_USERNAME=<email address> --from-literal=QUARKUS_MAILER_PASSWORD=<email password>`

### Step 6 - Deploy the application

   - Go to the application home directory and run:

      `./mvnw clean install -Dquarkus.kubernetes.deploy=true -Dquarkus.openshift.env.vars.quarkus-github-app-app-id=<your-github-app-id>`

   - You can also put the config properties to the `application.properties`

### Step 7 - Edit the WebHook URL in your GitHub application

1. Get the list of exposed routes:

   `oc get routes`

2. Edit the WebHook URL using the retrieved `HOST/PORT` value:

   `http://<HOST/PORT>`

And that's it. Again, try to create a PR to verify the format of the PR.
q
## Production deployment

We run WildFly GitHub Bot on Openshift in production. To save resources we also deploy it as native executable.

1. Log in into Openshift

   `oc login ...`


2. Then create the relevant secret:

   ```
   oc create secret generic wildfly-bot
   --from-literal=QUARKUS_GITHUB_APP_WEBHOOK_SECRET={TBD}
   --from-file=QUARKUS_GITHUB_APP_PRIVATE_KEY={TBD}
   --from-literal=QUARKUS_MAILER_FROM={TBD}
   --from-literal=QUARKUS_MAILER_USERNAME={TBD}
   --from-literal=QUARKUS_MAILER_PASSWORD={TBD}
   ```

3. Deploy the bot:

   ```
   ./mvnw clean install
   -Dquarkus.kubernetes.deploy=true
   -Dquarkus.openshift.env.vars.quarkus-github-app-app-id={TBD}
   -Dquarkus.native.container-build=true
   -Dnative
   ```