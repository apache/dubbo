#!/bin/sh
#  Licensed to the Apache Software Foundation (ASF) under one or more
#  contributor license agreements.  See the NOTICE file distributed with
#  this work for additional information regarding copyright ownership.
#  The ASF licenses this file to You under the Apache License, Version 2.0
#  (the "License"); you may not use this file except in compliance with
#  the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.


function fail_noclear {
    >&2 echo "    FATAL ERROR:
    ------------------
    $1"
    exit 1
}

function getProjectVersionFromPom {
    cat << EOF | xmllint --noent --shell pom.xml | grep content | cut -f2 -d=
setns pom=http://maven.apache.org/POM/4.0.0
xpath /pom:project/pom:version/text()
EOF
}

REPO=""
BRANCH=""

if [[ $# -eq 2 ]]; then
	REPO=$1
	BRANCH=$2
else
	fail_noclear "Please use ./release.sh YOUR_GIT_REPO YOUR_BRANCH_NAME."
fi

echo "Remote repo is: $REPO, branch is: $BRANCH"

read -p "Input the local directory to work on (default is $BRANCH): " DIR_NAME

DIR_NAME=${DIR_NAME:-$BRANCH}

echo $DIR_NAME

if [ -d $DIR_NAME ]; then
	#fail_noclear "$DIR_NAME exist, please use a new name."
	echo "$DIR_NAME exist, going on."
else
	#echo "$DIR_NAME exist, try to remove it."
	#rm -rf $DIR_NAME
	git clone -b $BRANCH $REPO $DIR_NAME
	if [ $? -ne 0 ]; then
		fail_noclear "git clone -b $BRANCH $REPO $DIR_NAME failed."
	fi
fi

cd $DIR_NAME

VERSION=$(getProjectVersionFromPom)
ORIGIN_VERSION="$VERSION"
while [[ ! $VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+(-M[0-9]+)?$ ]]
do
    read -p "Version to release (in pom now is $VERSION): " -e t1
    if [ -n "$t1" ]; then
      	VERSION="$t1"
    fi
done

TAG=dubbo-$VERSION
echo $TAG

# get last commit id
COMMIT_ID=`git rev-parse HEAD`
echo $COMMIT_ID

major_version=$(expr $VERSION : '\(.*\)\..*\..*')
minor_version=$(expr $VERSION : '.*\.\(.*\)\..*')
bugfix_version=$(expr $VERSION : '.*\..*\.\(.*\)')

next_version="$major_version.$minor_version.$(expr $bugfix_version + 1)-SNAPSHOT"
previous_minor_version=$(expr $bugfix_version - 1)
if [ $previous_minor_version -lt 0 ] ; then
    previous_version="$major_version.$minor_version.0-SNAPSHOT"
else
    previous_version="$major_version.$minor_version.$(expr $bugfix_version - 1)"
fi

echo $major_version $minor_version $bugfix_version $next_version $previous_minor_version $previous_version

read -p "Need to run mvn clean install -Prelease -Dmaven.test.skip=false ? (y/n, default is n) " NEED_INSTALL
echo $NEED_INSTALL
if [ "$NEED_INSTALL" = "y" ]; then
	echo "Start to mvn clean install"
	mvn clean install -Prelease -Dmaven.test.skip=false
else
	echo "Skip mvn clean install -Prelease -Dmaven.test.skip=false"
fi

read -p "Need to run mvn deploy $ORIGIN_VERSION to central repo ? (y/n, default is n) " NEED_DEPLOY

if [ "$NEED_DEPLOY" = "y" ]; then
	echo "Start to run mvn deploy SNAPSHOT"
	mvn deploy -Dmaven.test.skip=true
else
	echo "Skip deploy SNAPSHOT"
fi

echo "Start to run mvn release:clean..."
mvn release:clean
# remove local/remote tag
echo "Delete local tag $TAG"
git tag --delete $TAG

read -p "Operate remote $BRANCH-staging and tag $TAG ? (y/n, default is n) " FORCE_DELETE

if [ "$FORCE_DELETE" = "y" ]; then
	echo "Delete remote $BRANCH-staging and tag $TAG"
	git push origin --delete $TAG
	git push origin --delete $BRANCH-staging
fi

echo "Start to run mvn release:prepare..."
read -p "Input your github username: " USER_NAME
mvn release:prepare -Prelease -Darguments="-DskipTests" -DautoVersionSubmodules=true -Dusername=$USER_NAME -DupdateWorkingCopyVersions=true -DpushChanges=false -Dtag=$TAG -DreleaseVersion=$VERSION -DdevelopmentVersion=$next_version

if [ "$FORCE_DELETE" = "y" ]; then
	echo "Push local change to staging branch"
	git push origin $BRANCH:$BRANCH-staging

	# push tag to remote
	echo "Push tag $TAG to remote"
	git push origin $TAG
fi

if [ "$FORCE_DELETE" = "y" ]; then
	echo "Start to run mvn release:perform..."
	mvn -Prelease release:perform -Darguments="-DskipTests" -DautoVersionSubmodules=true -Dusername=$USER_NAME
else
	echo "Skip release"
fi

# reset working directory
echo "Reset local repo to $COMMIT_ID"
git reset --hard $COMMIT_ID

cd distribution/target
echo "Start to shasum for bin/source.zip"
shasum -b -a 512 apache-dubbo-${VERSION}-source-release.zip >> apache-dubbo-${VERSION}-source-release.zip.sha512
shasum -b -a 512 apache-dubbo-${VERSION}-bin-release.zip >> apache-dubbo-${VERSION}-bin-release.zip.sha512

read -p "Need to push bin/source.zip to Apache svn repo ? (y/n, default is n) " NEED_PUSH_APACHE

if [ "$NEED_PUSH_APACHE" = "y" ]; then
	# Need to test
	svn mkdir https://dist.apache.org/repos/dist/dev/incubator/dubbo/$VERSION -m "Create $VERSION directory"
	svn co --force --depth=empty https://dist.apache.org/repos/dist/dev/incubator/dubbo/$VERSION .
	svn add apache-dubbo-${VERSION}-source-release.zip
	svn add apache-dubbo-${VERSION}-source-release.zip.asc
	svn add apache-dubbo-${VERSION}-source-release.zip.sha512
	svn add apache-dubbo-${VERSION}-bin-release.zip
	svn add apache-dubbo-${VERSION}-bin-release.zip.asc
	svn add apache-dubbo-${VERSION}-bin-release.zip.sha512
	svn commit -m "Upload dubbo-$VERSION"

	echo "If this is your first release, make sure adding PUBLIC_KEY to KEYS manually."
else
	echo "Skip push bin/source.zip to Apache svn repo"
fi
