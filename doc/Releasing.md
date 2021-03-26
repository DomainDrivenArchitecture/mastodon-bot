# stable release (should be done from master)

```
#adjust [version]
vi package.json

lein release
git push --follow-tags

# bump version - increase version and add -SNAPSHOT
vi package.json
git commit -am "version bump"
git push
```