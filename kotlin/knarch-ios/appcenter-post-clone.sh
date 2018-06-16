set -e
pushd ..
./gradlew checkKonanCompiler
popd
pushd ..
./gradlew build
popd