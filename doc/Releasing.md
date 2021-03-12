# stable release (should be done from master)

```
#adjust [version]
vi package.json
vi project.clj

lein release

# bump version - increase version and add -SNAPSHOT
vi package.json
vi project.clj
git commit -am "version bump"
git push
```