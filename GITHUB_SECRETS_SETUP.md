# GitHub Secrets Setup Guide

This guide walks you through setting up the required GitHub secrets for CI/CD deployment.

## Why Do You Need This?

Without Docker Hub authentication, GitHub Actions will hit rate limits when building your Docker images:
- **Unauthenticated:** 100 pulls per 6 hours (you'll hit this quickly!)
- **Authenticated:** 200 pulls per 6 hours (free tier)

## Step-by-Step Setup

### 1. Create Docker Hub Access Token

1. **Go to Docker Hub:** https://hub.docker.com/settings/security
2. **Create New Token:**
   - Click "New Access Token"
   - Name: `github-actions` (or any name you prefer)
   - Permissions: Read-only is sufficient
3. **Copy the token** - You won't be able to see it again!

### 2. Add Secrets to GitHub Repository

1. **Go to your GitHub repository**
2. **Navigate to Settings:**
   - Click **Settings** (top menu)
   - Click **Secrets and variables** (left sidebar)
   - Click **Actions**
3. **Add First Secret:**
   - Click **New repository secret**
   - Name: `DOCKERHUB_USERNAME`
   - Value: Your Docker Hub username
   - Click **Add secret**
4. **Add Second Secret:**
   - Click **New repository secret**
   - Name: `DOCKERHUB_TOKEN`
   - Value: The access token you copied in step 1
   - Click **Add secret**

### 3. Verify Setup

Your secrets page should show:
- ✅ `DOCKERHUB_USERNAME`
- ✅ `DOCKERHUB_TOKEN`

**Note:** You won't be able to see the values after adding them (GitHub hides them for security).

### 4. Test the Workflow

```bash
# Make a small change and push
git add .
git commit -m "Test CI/CD workflow"
git push origin main

# Watch the workflow run
# Go to: GitHub → Actions tab
```

If you see "Error: You have reached your pull rate limit", the secrets are not set up correctly.

## Troubleshooting

### "Error: You have reached your pull rate limit"

**Solution:** Check that both secrets are added correctly:
1. Go to Settings → Secrets and variables → Actions
2. Verify both `DOCKERHUB_USERNAME` and `DOCKERHUB_TOKEN` exist
3. If they exist but workflow still fails, delete them and recreate (token might be invalid)

### "Error: unauthorized: incorrect username or password"

**Possible causes:**
- Wrong Docker Hub username (it's case-sensitive!)
- Invalid or expired access token
- Using password instead of access token (must use token, not password!)

**Solution:**
1. Delete both secrets
2. Create a new access token on Docker Hub
3. Re-add both secrets with correct values

### Workflow succeeds but takes very long

This is normal for the first run! Subsequent runs are faster due to layer caching.

## Security Notes

✅ **Safe to commit:**
- GitHub Actions workflow file (`.github/workflows/docker-build-push.yml`)
- This documentation

❌ **NEVER commit:**
- Docker Hub access tokens
- Docker Hub passwords
- Any credentials in code

GitHub secrets are encrypted and only accessible during workflow runs.

## Next Steps

After setting up secrets:
1. Push to `main` branch to trigger the workflow
2. Check the Actions tab to see the build progress
3. Once complete, Watchtower on your server will auto-deploy (within 5 minutes)
4. Verify deployment: `docker compose -f docker-compose.ghcr.yml ps`

For more details, see [DEPLOYMENT.md](DEPLOYMENT.md).
