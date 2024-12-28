# Contributing to Trade Bot

Thank you for considering contributing to Trade Bot! By helping improve this project, you are directly contributing to its success and providing valuable tools for the community. This guide will help you get started.

## Prerequisites
To contribute, ensure your environment meets the following requirements:
- **Java Version**: 17
- **Build Tool**: Maven
- **IDE**: IntelliJ IDEA (recommended)
- **Operating System**: Any (Windows, MacOS, Linux)

## Setting Up the Project Locally
1. **Clone the Repository**:
   ```bash
   git clone https://github.com/n1ceSmoke/tradeBot.git
   ```
2. **Create a PostgreSQL Database**:
    - Set up a PostgreSQL database instance.
    - Note down the database URL, username, and password for configuration.

3. **Generate Binance API Keys**:
    - Create an account on Binance (if you don’t already have one).
    - Generate API keys for the bot to use.

4. **Add `application.properties`**:
    - Refer to the example provided in the `README.md` file.
    - Configure database credentials and Binance API keys.

5. **Build the Project**:
   ```bash
   mvn clean package
   ```

6. **Run the Application**:
   ```bash
   java -jar target/trade.bot-0.0.1.jar
   ```

## Code Style and Standards
We follow the **Google Java Style Guide**. Please ensure that your code adheres to the following conventions:
- **Methods and Variables**: Use `camelCase`.
- **Classes**: Use `PascalCase`.
- **General Style**: Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).

## Workflow for Contributions
1. **Create a New Branch**:
    - Use the naming convention: `feature/your-feature-name`.
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make Your Changes**:
    - Ensure your changes adhere to the coding standards.
    - Test your changes thoroughly.

3. **Submit a Pull Request (PR)**:
    - Push your branch to your fork and create a pull request.
    - Clearly describe the changes and their purpose in the PR.
    - Add the **Code Owner** as a Reviewer.

## Reporting Issues
If you encounter an issue, please create an issue on GitHub with the following details:
1. **Description** of the issue.
2. **Steps to reproduce** the problem.
3. **Logs** (if applicable).
4. **Screenshots or screen recordings** (if possible).

## Feedback and Suggestions
Your feedback is valuable! Feel free to open a discussion or an issue if you have ideas for improvement.

---
Thank you for contributing to Trade Bot! Together, we can build a better project.

