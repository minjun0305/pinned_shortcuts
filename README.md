# Flutter Pinned Shortcuts 📌

[![Pub](https://img.shields.io/pub/v/flutter_pinned_shortcuts.svg)](https://pub.dev/packages/flutter_pinned_shortcuts)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

✨ A Flutter plugin that lets you create Android pinned shortcuts with style! Pin your app's most important features directly to the user's home screen.


## ✅ Features

- 📱 Create pinned shortcuts on Android home screens
- 🖼️ Support for various image sources:
    - 📦 Flutter assets
    - 🤖 Android resources
    - 🌐 Network images (auto-downloaded)
    - 📂 File paths
- 👂 Listen to shortcut clicks in Flutter
- 🔍 Check if a shortcut is pinned

## 🚀 Getting Started

### 📥 Installation

Add this to your package's `pubspec.yaml` file:

```yaml
dependencies:
  flutter_pinned_shortcuts: ^0.0.1
```

Then run:

```bash
$ flutter pub get
```

### 📱 Android Setup

Ensure your `minSdkVersion` is at least 16 (Android 4.1) in your app's `android/app/build.gradle` file:

```gradle
android {
    defaultConfig {
        minSdkVersion 16
        // ...
    }
}
```

For optimal functionality, Android 8.0 (API level 26) or higher is recommended.

## 💻 Usage

### 🚀 Initialize the plugin

First, initialize the plugin (typically in your `main.dart`):

```dart
import 'package:flutter_pinned_shortcuts/flutter_pinned_shortcuts.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
  // Initialize the plugin
  await FlutterPinnedShortcuts.initialize();
  
  runApp(MyApp());
}
```

### ✅ Check if pinned shortcuts are supported

```dart
bool isSupported = await FlutterPinnedShortcuts.isSupported();
if (isSupported) {
  print('🎉 Pinned shortcuts are supported!');
} else {
  print('😔 Pinned shortcuts are not supported on this device');
}
```

### 📌 Create a pinned shortcut

#### With a Flutter asset image

```dart
await FlutterPinnedShortcuts.createPinnedShortcut(
  id: 'asset_shortcut',
  label: 'My Shortcut',
  imageSource: 'assets/icon.png',
  imageSourceType: ImageSourceType.asset,
  longLabel: 'My Flutter Asset Shortcut', // Optional
  extraData: {'source': 'asset'}, // Optional data to pass when shortcut is clicked
);
```

#### With an Android resource image

```dart
await FlutterPinnedShortcuts.createPinnedShortcut(
  id: 'resource_shortcut',
  label: 'Resource Shortcut',
  imageSource: 'ic_launcher', // drawable asset name
  imageSourceType: ImageSourceType.resource,
);
```

#### With a network image

```dart
await FlutterPinnedShortcuts.createPinnedShortcut(
  id: 'network_shortcut',
  label: 'Network Shortcut',
  imageSource: 'https://example.com/image.png',
  imageSourceType: ImageSourceType.network,
);
```

#### With a file path

```dart
await FlutterPinnedShortcuts.createPinnedShortcut(
  id: 'file_shortcut',
  label: 'File Shortcut',
  imageSource: '/path/to/file.png',
  imageSourceType: ImageSourceType.file,
);
```

### 👂 Listen to shortcut clicks

Set up a listener for when shortcuts are clicked:

```dart
FlutterPinnedShortcuts.onShortcutClick.listen((shortcutId) {
  print('🎉 Shortcut clicked: $shortcutId');
  // Handle the shortcut click, e.g., navigate to a specific screen
});
```

### 🔍 Check if a shortcut is pinned

```dart
bool isPinned = await FlutterPinnedShortcuts.isPinned('shortcut_id');
if (isPinned) {
  print('📌 Shortcut is pinned!');
} else {
  print('🚫 Shortcut is not pinned');
}
```

### ♻️ Dispose the plugin

```dart
// Call this when you're done with the plugin, typically in the dispose method
@override
void dispose() {
  FlutterPinnedShortcuts.dispose();
  super.dispose();
}
```

## 📱 Complete Example

```dart
import 'package:flutter/material.dart';
import 'package:flutter_pinned_shortcuts/flutter_pinned_shortcuts.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
  // Initialize the plugin
  await FlutterPinnedShortcuts.initialize();
  
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _shortcutStatus = 'No shortcut clicked yet';
  bool _isSupported = false;
  final String _shortcutId = 'test_shortcut';

  @override
  void initState() {
    super.initState();
    _checkSupport();
    _setupShortcutListener();
  }

  // Check if pinned shortcuts are supported on this device
  Future<void> _checkSupport() async {
    final isSupported = await FlutterPinnedShortcuts.isSupported();
    setState(() {
      _isSupported = isSupported;
    });
  }

  // Set up a listener for shortcut clicks
  void _setupShortcutListener() {
    FlutterPinnedShortcuts.onShortcutClick.listen((shortcutId) {
      setState(() {
        _shortcutStatus = 'Shortcut clicked: $shortcutId';
      });
    });
  }

  // Create a pinned shortcut with a Flutter asset image
  Future<void> _createShortcut() async {
    await FlutterPinnedShortcuts.createPinnedShortcut(
      id: _shortcutId,
      label: 'Asset Shortcut',
      imageSource: 'assets/icon.png',
      imageSourceType: ImageSourceType.asset,
      extraData: {'source': 'asset'},
    );
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Flutter Pinned Shortcuts Example'),
        ),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(
                'Pinned Shortcuts ${_isSupported ? 'are supported ✅' : 'are NOT supported ❌'} on this device',
                style: Theme.of(context).textTheme.titleMedium,
              ),
              const SizedBox(height: 20),
              Text(
                _shortcutStatus,
                style: Theme.of(context).textTheme.bodyLarge,
              ),
              const SizedBox(height: 30),
              ElevatedButton(
                onPressed: _isSupported ? _createShortcut : null,
                child: const Text('Create Shortcut 📌'),
              ),
            ],
          ),
        ),
      ),
    );
  }

  @override
  void dispose() {
    FlutterPinnedShortcuts.dispose();
    super.dispose();
  }
}
```

## 📚 API Reference

### FlutterPinnedShortcuts

| Method | Description |
| ------ | ----------- |
| `initialize()` | 🚀 Initialize the plugin and set up shortcut click listener |
| `isSupported()` | ✅ Check if pinned shortcuts are supported on the device |
| `createPinnedShortcut()` | 📌 Create a new pinned shortcut |
| `isPinned()` | 🔍 Check if a shortcut is pinned |
| `dispose()` | ♻️ Clean up resources used by the plugin |

### ImageSourceType

| Value | Description |
| ----- | ----------- |
| `asset` | 📦 Flutter asset from the assets folder |
| `resource` | 🤖 Android resource from res folder |
| `network` | 🌐 Image from a network URL |
| `file` | 📂 Image from a file path |

## ⚠️ Known Limitations

- 📱 Android only - not available on iOS
- 🧪 Full functionality requires Android 8.0 (API 26) or higher
- 🔄 Older Android versions have limited support for shortcut management
- 🖼️ For best results, shortcut icons should be 48x48dp or 96x96px

## 🔮 Future Plans

- [ ] Support for dynamic shortcuts
- [ ] Support for adaptive icons
- [ ] Multiple shortcut creation at once
- [ ] Improved error handling and reporting
- [ ] More customization options for shortcut appearance

## 🤝 Contributing

Contributions are welcome! Feel free to submit a Pull Request.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📱 Compatibility

| Android Version | API Level | Support Level |
| --------------- | --------- | ------------ |
| Android 8.0+ | 26+ | ✅ Full support |
| Android 7.1 | 25 | ⚠️ Limited support |
| Android 7.0 - 4.1 | 24-16 | ⚠️ Basic support |
| Android < 4.1 | <16 | ❌ Not supported |

## 📮 Contact

Have questions or suggestions? Please open an issue on GitHub!

---

Made with ❤️ by [Your Name](https://github.com/lkrjangid1)