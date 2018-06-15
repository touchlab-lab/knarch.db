set -e
pushd cpp
./gradlew $1
popd
pushd kotlin
./gradlew $1
popd
