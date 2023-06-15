# 📍 Nearby Places App

The Nearby Places Android App is a location-based application that allows users to discover nearby points of interest based on their current location. The app utilizes the 🗺️ Google Maps Android API to display the user's current location and markers for various points of interest. It also integrates a third-party API, such as the Google Places API, to fetch additional details about each location.

## Features

- 📌 **Display Current Location**: The app displays the user's current location on a Google Map.

- 🔍 **Fetch Nearby Points of Interest**: Using a third-party API, the app fetches nearby points of interest based on the user's location and a predefined search radius.

- 📍 **Marker Display**: The app marks the points of interest as markers on the Google Map.

- ℹ️ **Detailed Information**: When a marker is clicked, a bottom sheet or dialog pops up with more information about the selected point of interest, like its name, address, rating, etc.

- 🔖 **Category Filtering**: Users can filter the points of interest by category, such as restaurants, parks, or museums.

- 🚗 **Get Directions**: The app allows users to get directions to a selected point of interest using Google Maps or another navigation app.

## Requirements

To build and run the app, ensure that you have the following:

- 🛠️ Android Studio: The latest version of Android Studio should be installed on your development machine.

- 📱 Android Device: An Android device running Android 7.0 (API level 24) or higher, or an Android emulator configured in Android Studio.

- 🔑 Google Maps API Key: Obtain an API key from the Google Cloud Console to enable the Google Maps functionality in the app.

## Setup Instructions

Follow these steps to set up and run the Nearby Points of Interest Android App:

1. 📥 Clone the repository to your local machine.

2. 🏗️ Open Android Studio and select "Open an Existing Project" from the welcome screen. Browse to the cloned repository's directory and open the project.

3. 🌐 In the project root directory, locate the `local.defaults.properties` file.

4. 🔑 Replace the placeholder `YOUR_API_KEY` with your actual Google Maps API key obtained from the Google Cloud Console.

5. 📱 Connect your Android device to your development machine or start an Android emulator.

6. 🏗️ Build the project by selecting "Build > Make Project" from the menu bar or by pressing `Ctrl + F9` (Windows/Linux) or `Cmd + F9` (Mac).

7. ▶️ Run the app on your connected Android device or emulator by selecting "Run > Run 'app'" from the menu bar or by pressing `Shift + F10` (Windows/Linux) or `Ctrl + R` (Mac).

8. 🎉 The app should now be installed and running on your device or emulator.

## Design Decisions

During the development of the Nearby Points of Interest Android App, the following design decisions were made:

- 🗺️ **Google Maps Android API**: The Google Maps Android API was chosen for its extensive features and ease of integration with Android applications. It provides reliable maps and location-based services, allowing the app to display the user's current location and nearby points of interest.

- 🌐 **Third-Party API**: A third-party API, such as the Google Places API, was utilized to fetch additional information about the points of interest. This decision was made to enhance the app's functionality and provide users with detailed data about each location, including names, addresses, ratings, and photos.

- 🔍 **Search View**: A search view was implemented to enable users to search for specific places or locations. This feature enhances usability by allowing users to quickly find points of interest based on their search queries.

Enjoy exploring nearby points of interest with the Nearby Points of Interest Android App! 🌟🚀