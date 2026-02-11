Contributing to WildFly Github Bot
==================================

Welcome to the WildFly Github Bot project and thank you for deciding to contribute to this project! We welcome contributions from the community. This guide will walk you through the steps for getting started on our project.

- [Legal](#legal)
- [Forking the Project](#forking-the-project)
- [Issues](#issues)
    - [Good First Issues](#good-first-issues)
- [Setting up your Developer Environment](#setting-up-your-developer-environment)
- [Contributing Guidelines](#contributing-guidelines)

## Legal

All contributions to this repository are licensed under the [Apache License](https://www.apache.org/licenses/LICENSE-2.0), version 2.0 or later, or, if another license is specified as governing the file or directory being modified, such other license.

All contributions are subject to the [Developer Certificate of Origin (DCO)](https://developercertificate.org/).
The DCO text is also included verbatim in the [dco.txt](dco.txt) file in the root directory of the repository.

### Compliance with Laws and Regulations

All contributions must comply with applicable laws and regulations, including U.S. export control and sanctions restrictions.
For background, see the Linux Foundation’s guidance:
[Navigating Global Regulations and Open Source: US OFAC Sanctions](https://www.linuxfoundation.org/blog/navigating-global-regulations-and-open-source-us-ofac-sanctions).


## Forking the Project
First of all, you will need to fork the [repository](https://github.com/wildfly/wildfly-github-bot).

This can be done by going to your newly forked repository, which should be at `https://github.com/USERNAME/wildfly-github-bot`.

Then, there will be a green button that says "Code". Click on that and copy the URL.

Then, in your terminal, paste the following command:
```bash
git clone [URL]
```
Be sure to replace [URL] with the URL that you copied.

Now you have this repository on your computer!

## Issues
The WildFly Github bot project currently uses Github issues as means of filing new issues.

To create an issue simply click on tab "Issues" and then on the button "New issue"

### Good First Issues
If you would like to contribute, however you are not quite confident, do not hesitate to start with the `good-first-issue` labeled issues. These are a triaged set of issues that are great for getting started on our project. These can be found [here](https://github.com/wildfly/wildfly-github-bot/labels/good%20first%20issue).

Once you have selected an issue you'd like to work on, make sure you write a comment to the corresponding issue stating, you would like to work on it, so others are aware of it such as [this](https://github.com/wildfly/wildfly-github-bot/issues/44).

It is recommended that you use a separate branch for every issue you work on. We would recommend you naming the branch corresponding to the issue you are solving. This can be achieved with `git checkout -b issue-123`

## Setting up your Developer Environment
You will need:

* JDK 17
* Git
* Maven 3.8.6 or later
* An [IDE](https://en.wikipedia.org/wiki/Comparison_of_integrated_development_environments#Java)
  (e.g., [IntelliJ IDEA](https://www.jetbrains.com/idea/download/), [Eclipse](https://www.eclipse.org/downloads/), [VSCode](https://code.visualstudio.com/download) etc.)

First `cd` to the directory where you cloned the project (eg: `cd wildfly-github-bot`)

Add a remote ref to upstream, for pulling future updates, i.e. synchronizing this repository.
For example:

```
git remote add upstream https://github.com/wildfly/wildfly-github-bot.git
```

## Running the github bot
Not so quick coding hero! You will need to create your own instance of the github app to be able to run the github bot. Please follow this [guide](README.md)
> Note: This is not needed for test mode

Finally, we can run the github bot:
```bash
./mvnw clean quarkus:dev
```

Or to run only tests use:

```bash
./mvnw clean quarkus:test
```

## Contributing Guidelines

When submitting a PR, please keep the following guidelines in mind:

1. In general, it's good practice to squash all of your commits into a single commit. For larger changes, it's ok to have multiple meaningful commits. If you need help with squashing your commits, feel free to ask us how to do this on your pull request. We're more than happy to help!

2. Please include the github issue you worked on in the description of your pull request and in your commit message. For example, working on issue 123 would result in a description stating something like `Resolves #123`. This is done in order to automatically link issues and close them when merging the Pull Request, see [here](https://docs.github.com/en/issues/tracking-your-work-with-issues/linking-a-pull-request-to-an-issue#linking-a-pull-request-to-an-issue-using-a-keyword)

3. If it so happens that you solved multiple issues in the same Pull Request, please include all of them in the description.

For example a Pull Request can look like [this](https://github.com/wildfly/wildfly-github-bot/pull/147)