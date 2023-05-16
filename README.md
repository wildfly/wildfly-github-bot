# wildfly-github-bot
wildfly-github-bot helps you to keep your pull requests in the correct format.

This project is build with usage of Quarkus GitHub App: https://quarkiverse.github.io/quarkiverse-docs/quarkus-github-app/dev/index.html

## Development
###Step 1 - Register the application
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
- `Metadata` - `Access: Read-only`
- `Pull requests` - `Access: Read and write`
5. Subscribe to events
- `Pull requests`
- `Pull request review comment`
6. Create a private key

   After creating the app scroll down and press `Generate a private key`

   You will be asked to download the key, keep it around, we will need it for the next stet.

###Step 2 - Set up the app
As the configuration is environment-specific, and you probably don’t want to commit it in your repository, the best is to create in the root a `.env` file.

The content of your .env file should be as follows:

```
QUARKUS_GITHUB_APP_APP_ID=<the numeric app id>
QUARKUS_GITHUB_APP_APP_NAME=<the name of your app>
QUARKUS_GITHUB_APP_WEBHOOK_PROXY_URL=<your Smee.io channel URL>
QUARKUS_GITHUB_APP_PRIVATE_KEY=-----BEGIN RSA PRIVATE KEY-----\
                 <your private key>                          \
-----END RSA PRIVATE KEY-----
```

**QUARKUS_GITHUB_APP_APP_ID**
The numeric app id appears in the App ID field.

**QUARKUS_GITHUB_APP_APP_NAME**
The name of your app is the one appearing in the GitHub URL. It is only used to improve usability in dev mode.

**QUARKUS_GITHUB_APP_WEBHOOK_PROXY_URL**
The URL you obtained when you created your Smee.io channel.

**QUARKUS_GITHUB_APP_PRIVATE_KEY**
The content of the private key you generated and downloaded. Open the key file with a text editor as key viewers usually only show fingerprints.

###Step 3 - Set up the app
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
   title-check:
     pattern: "\\[WFLY-\\d+\\]\\s+.*|WFLY-\\d+\\s+.*"
     message: "Wrong content of the title!"
   description:
     pattern: "JIRA:\\s+https://issues.jboss.org/browse/WFLY-\\d+|https://issues.jboss.org/browse/WFLY-\\d+"
     message: "The PR description must contain a link to the JIRA issue"
   commits-quantity:
     quantity: "1-3"
     message: "Too many commits in PR!"
```

1. `title-check`- Checks the title of a PR by using a regular expression in the `pattern` field.
> The correct format in example is "[WFLY-11] Name"
2. `description`- Checks comments of a PR by using regular expressions in the `pattern` field.
> The correct format in example is "https://issues.jboss.org/browse/WFLY-11"
3. `commits-quantity`- Checks the amount of commits in PR with the amount in the `quantity` field.
> In the field you can use the exact values '1', '2' or range '1-2', '2-4' up to 100.
4. `message` - The text of an error message in the respective check.

Also, there is a possibility to select checks that you need. Just left in the `wildfly-bot.yml` file checks you need.

Like this:
```
wildfly:
 rules:
   - title: "test"
   - body: "test"
     notify: [xstefank,petrberan]
 format:
   title-check:
     pattern: "\\[WFLY-\\d+\\]\\s+.*|WFLY-\\d+\\s+.*"
     message: "Wrong content of the title!"
```

### Run the application in dev mode

Run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Dev UI available in dev mode only at http://localhost:8080/q/dev/.

**DONE**

### DEPLOY ON OPENSHIFT

**REQUIREMENTS**
 - JDK 17+ with **JAVA_HOME** configured appropriately
 - OpenShift
 - [OpenShift CLI](https://docs.openshift.com/container-platform/4.7/cli_reference/openshift_cli/getting-started-cli.html) 
 - Apache Maven 3.8.6

**STEP 1 - Register new GitHub App**

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

**STEP 2 - Generate a private key**
 - Scroll down to generate a private key
 - Download it as you will need it later

**STEP 3 - Install the application on the desired repository**
 - You can find this in the Install App tab of your GitHub application

**STEP 4 - Log into the OpenShift cluster**
 - `oc login -u <username>`
   - You will need to fill required information in prompt
 - `oc login --token=<token> --server=<serverUrl>`
   - You can request the token via the `Copy Login Command` link in the OpenShift web console.



**STEP 5 - Create OpenShift secret with webhook secret and private key**


      `oc create secret generic <secret-name> --from-literal=QUARKUS_GITHUB_APP_WEBHOOK_SECRET=<your-webhook-secret> --from-file=QUARKUS_GITHUB_APP_PRIVATE_KEY=<path-to-your-private-key>`

**STEP 6 - Deploy the application**
   - Go to the application home directory and run:


      `./mvnw clean install -Dquarkus.kubernetes.deploy=true -Dquarkus.openshift.env.vars.quarkus-github-app-app-id=<your-github-app-id> -Dquarkus.openshift.env.secrets=<secret-name>`
   - You can also put the config properties to the `application.properties`

**STEP 7 - Edit the WebHook URL in your GitHub application**
1. Get the list of exposed routes:

   `oc get routes`
2. Edit the WebHook URL using the retrieved `HOST/PORT` value:

   `http://<HOST/PORT>`
### DONE