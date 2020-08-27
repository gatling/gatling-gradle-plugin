#!/bin/sh

if [ $# -ne 1 ]; then
    echo "Usage: bootstrap.sh SAMPLE_PROJECT_FOLDER"
    exit 1
fi

DEST="$1"

if [ -a "$DEST" ]; then
    echo "$DEST already exists"
    exit 2
fi

mkdir -p "$DEST/tmp"
cd "$DEST/tmp"

git init -q
git remote add origin https://github.com/gatling/gatling-gradle-plugin.git
git config core.sparseCheckout true
echo "src/main/bootstrap/" >> .git/info/sparse-checkout
git pull -q origin bootstrap_project

cp -R src/main/bootstrap/* ..
cd ..
rm -rf tmp

rm -f .gitignore || true

echo '#' >> .gitignore
echo '# From https://github.com/github/gitignore/blob/master/Gradle.gitignore' >> .gitignore
echo '#' >> .gitignore
curl -sL https://raw.githubusercontent.com/github/gitignore/master/Gradle.gitignore >> .gitignore

echo '#' >> .gitignore
echo '# From https://github.com/github/gitignore/blob/master/Java.gitignore' >> .gitignore
echo '#' >> .gitignore
curl -sL https://raw.githubusercontent.com/github/gitignore/master/Java.gitignore >> .gitignore

echo '#' >> .gitignore
echo '# From https://github.com/github/gitignore/blob/master/Global/JetBrains.gitignore' >> .gitignore
echo '#' >> .gitignore
curl -sL https://raw.githubusercontent.com/github/gitignore/master/Global/JetBrains.gitignore >> .gitignore

echo '#' >> .gitignore
echo '# From https://github.com/github/gitignore/blob/master/Global/Eclipse.gitignore' >> .gitignore
echo '#' >> .gitignore
curl -sL https://raw.githubusercontent.com/github/gitignore/master/Global/Eclipse.gitignore >> .gitignore

echo
echo 'Created sample Gatling project for Gradle.'
echo 'Use following to run sample simulations.'
echo
echo "  $ cd $DEST"
echo '  $ ./gradlew gatlingRun'
echo
