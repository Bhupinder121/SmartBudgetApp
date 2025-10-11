# SmartBudget - Android Finance Tracker

[![Build Status](https://img.shields.io/github/actions/workflow/status/YOUR_USERNAME/YOUR_REPONAME/android.yml?branch=main)](https://github.com/YOUR_USERNAME/YOUR_REPONAME/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

SmartBudget is a modern Android application designed to help users track their income and expenses, set budgets, and gain clear insights into their spending habits. With a clean, dark-themed UI and seamless Google Sign-In, managing personal finances becomes simple and intuitive.

## Screenshots

| Home Screen                                     | Transactions Screen                               | Budgets Screen                                  |
| :----------------------------------------------: | :-----------------------------------------------: | :---------------------------------------------: |
| *(Your Screenshot Here)*                        | *(Your Screenshot Here)*                         | *(Your Screenshot Here)*                       |
| **Fig 1.** At-a-glance financial summary.        | **Fig 2.** Detailed and filterable transaction list. | **Fig 3.** Tools for setting spending limits.    |

## Features

-   **Secure Authentication:** Fast and secure sign-up and sign-in using **Google Sign-In** and the modern **Credential Manager API**.
-   **Dashboard:** A comprehensive home screen showing total balance, monthly income/expense summary, daily spending limit progress, and a list of today's transactions.
-   **Transaction Management:**
    -   Add new income or expense transactions through a simple dialog.
    -   View a complete history of all transactions.
    -   **Filter transactions** by type (Income/Expense) and date range (7 days, 1 Month, 1 Year).
-   **Budgeting Tools:**
    -   Set and update a **daily spending limit**.
    -   Track your total monthly spending against a monthly budget with a visual progress bar.
-   **Data Synchronization:** All financial data is securely synced with a backend server, ensuring your data is always available.
-   **Modern UI:** A sleek, card-based dark-mode interface built with Material Design components for an intuitive user experience.

## Tech Stack & Architecture

This project follows modern Android development practices and leverages a robust set of libraries and tools.

-   **Language:** **Java**
-   **Architecture:** **MVVM (Model-View-ViewModel)** - A clean and scalable architecture that separates UI from business logic.
-   **UI:**
    -   **Android XML Layouts** with `ConstraintLayout` for responsive UIs.
    -   **Material Design Components** for a consistent and modern look and feel.
    -   `RecyclerView` for displaying dynamic lists efficiently.
-   **Navigation:** **Android Navigation Component** for managing fragment transactions and navigation flow.
-   **Networking:**
    -   **Retrofit 2** for type-safe HTTP requests to the backend API.
    -   **Gson** for seamless JSON serialization and deserialization.
-   **Authentication:**
    -   **Google Play Services Auth** for Google Sign-In.
    -   **AndroidX Credential Manager** for a unified authentication API.
-   **Build Tool:** **Gradle**

## Setup and Installation

To get the project running on your local machine, follow these steps:

**1. Clone the repository:**


**2. Google Services Configuration:**
This project uses Google Sign-In, which requires configuration through the Google Cloud Console / Firebase.
-   Go to the [Firebase Console](https://console.firebase.google.com/) and create a new project.
-   Add an Android app to the project with the package name `com.example.smartbugdet`.
-   Follow the instructions to download the `google-services.json` file.
-   Place the downloaded `google-services.json` file in the `app/` directory of the project.

**3. Configure Backend URL:**
The app needs to know the base URL of your backend server.
-   Open the `app/src/main/java/com/example/smartbugdet/network/RetrofitClient.java` file.
-   Change the `BASE_URL` constant to point to your deployed backend.
