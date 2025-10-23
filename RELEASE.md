# Release Checklist for SpawnElytra

## Pre-Release Preparation

### Code Quality
- [x] Clean build artifacts removed
- [x] All source files properly organized 
- [x] No unnecessary files in repository
- [x] .gitignore properly configured
- [x] Code compiles without errors

### Documentation
- [x] README.md updated with current features
- [x] Installation instructions clear
- [x] Configuration documentation complete
- [x] Requirements clearly stated

### Project Configuration
- [x] plugin.yml metadata complete and accurate
- [x] Version consistency across files (build.gradle.kts, plugin.yml)
- [x] GitHub workflows updated for Java 21
- [x] Release workflow configured

## Release Process

### GitHub Release
1. **Create and push a version tag:**
   ```bash
   git tag v2.0
   git push origin v2.0
   ```

2. **Automatic release creation** - The GitHub workflow will:
   - Build the plugin
   - Create a GitHub release
   - Upload the JAR file as `SpawnElytra-2.0.jar`

### Manual Release (Alternative)
1. **Build the plugin:**
   ```bash
   ./gradlew clean build
   ```

2. **Create GitHub release:**
   - Go to GitHub repository → Releases → Create new release
   - Tag: `v2.0`
   - Title: `SpawnElytra v2.0`
   - Upload `build/libs/craftattackspawnelytra-2.0.jar`
   - Rename to `SpawnElytra-2.0.jar`

## Platform Publishing

### SpigotMC
1. Go to [SpigotMC Resources](https://www.spigotmc.org/resources/)
2. Create new resource or update existing
3. Upload JAR file
4. Update description with features
5. Set correct Minecraft version compatibility
6. Add screenshots if available

### Modrinth
1. Go to [Modrinth](https://modrinth.com/)
2. Create new project or update existing
3. Upload JAR file
4. Configure project details:
   - Categories: Server Optimization, Utility
   - Environment: Server
   - Minecraft versions: 1.21+
   - Mod loaders: Spigot, Paper, etc.

### Hangar (PaperMC)
1. Go to [Hangar](https://hangar.papermc.io/)
2. Create new project
3. Upload plugin
4. Set appropriate tags and categories

## Post-Release

### Verification
- [ ] GitHub release created successfully
- [ ] JAR file downloadable
- [ ] Plugin loads on test server
- [ ] All features working as expected

### Documentation Updates
- [ ] Update any external documentation
- [ ] Announce release on relevant forums/Discord
- [ ] Update project status if needed

### Next Version Planning
- [ ] Plan next features
- [ ] Update version numbers for next release
- [ ] Create milestone for next version

---

## Important Notes

- **Version Format:** Use semantic versioning (v2.0, v2.1, v3.0)
- **JAR Naming:** Release JARs should be named `SpawnElytra-{version}.jar`
- **Compatibility:** Test on latest Spigot/Paper versions
- **License:** Maintain attribution to original author CoolePizza

## Common Issues

### Build Failures
- Ensure Java 21 is available
- Check Gradle wrapper permissions: `chmod +x gradlew`
- Verify internet connection for dependency downloads

### Upload Issues
- Check file size limits on platforms
- Ensure JAR is not corrupted
- Verify file naming conventions