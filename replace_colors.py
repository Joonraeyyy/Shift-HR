import re

files_to_process = [
    "app/src/main/java/com/example/MainActivity.kt",
    "app/src/main/java/com/example/ui/SaaSScreens.kt",
    "app/src/main/java/com/example/ui/PlatformGuideScreen.kt",
    "app/src/main/java/com/example/ui/Top5DashboardScreen.kt",
    "app/src/main/java/com/example/ui/PerformanceReportingScreen.kt"
]

for filepath in files_to_process:
    print(f"Processing {filepath}...")
    with open(filepath, 'r') as f:
        content = f.read()

    # Pattern for color = Color.White
    content = re.sub(r'color\s*=\s*Color\.White\b', 'color = com.example.ui.theme.AppTextColor', content)
    
    # Pattern for color = Color.White.copy(
    content = re.sub(r'color\s*=\s*Color\.White\.copy\(', 'color = com.example.ui.theme.AppTextColor.copy(', content)

    # Pattern for tint = Color.White
    content = re.sub(r'tint\s*=\s*Color\.White\b', 'tint = com.example.ui.theme.AppTextColor', content)

    # Pattern for tint = Color.White.copy(
    content = re.sub(r'tint\s*=\s*Color\.White\.copy\(', 'tint = com.example.ui.theme.AppTextColor.copy(', content)

    # Pattern for textColor = Color.White
    content = re.sub(r'textColor\s*=\s*Color\.White\b', 'textColor = com.example.ui.theme.AppTextColor', content)

    # Pattern for textColor = Color.White.copy(
    content = re.sub(r'textColor\s*=\s*Color\.White\.copy\(', 'textColor = com.example.ui.theme.AppTextColor.copy(', content)

    with open(filepath, 'w') as f:
        f.write(content)

print("Done replacing colors!")
