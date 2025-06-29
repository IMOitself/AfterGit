# AfterGit

**simplest git client that uses Termux's API**

> [!TIP]
> [implementation of AfterRun](https://github.com/IMOitself/AfterRun)
> [!NOTE]
> this is the AIDE version of [AfterGit](/storage/emulated/0/AppProjects/AfterGit-Official) which is a Gradle project.


## Download

[![Releases](https://img.shields.io/badge/Releases-look%20for%20apk-blue?style=for-the-badge)](https://github.com/IMOitself/AfterGit/releases/)

<br>

## Installation
> [!IMPORTANT]
> must have [Termux](https://f-droid.org/en/packages/com.termux/)   installed <br><br>
>  must have run this command on Termux first:
> ```bash
> pkg install termux-api
> sed -i 's/# allow-external-apps = true/allow-external-apps = true/g' ~/.termux/termux.properties
> termux-setup-storage
> ```
<br>

## Features

- [x] check repo status
- [x] list unsaved changes
- [x] commit changes
- [x] commit amend
- [x] show unsaved changes' diff
- [x] commit history
- [x] commit history graph
- [x] commit diff
- [ ] push
- [ ] fetch
- [ ] pull

<br>

## Screenshots

> [!TIP]
> idk i just like the green alert thingy here :D

![ss1](assets/ss1_home.jpg)
![ss2](assets/ss2_commit.jpg)
![ss3](assets/ss3_diff.jpg)
![ss4](assets/ss4_logs.jpg)
![ss5](assets/ss5_logs.jpg)
![ss6](assets/ss6_commit_desc.jpg)
![ss7](assets/ss7_status.jpg)
![ss8](assets/ss8_coming_soon.jpg)
