# stable release (should be done from master)

```
#adjust [version]
vi package.json
vi project.clj

git commit -am "release"
git tag -am "release" [release version no]
git push --follow-tags

# bump version - increase version and add -SNAPSHOT
vi package.json
vi project.clj
git commit -am "version bump"
git push
```