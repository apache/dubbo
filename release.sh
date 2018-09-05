#!/bin/bash
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

# set -e
# set -x

function fail {
    >&2 echo "\033[31m\033[01m[
    FATAL ERROR:
    ------------------
    $1 ]\033[0m"

    echo "Clear current work dir"
    git add .
    git commit -m 'Failed preparation for release.'
    exit 1
}

function fail_noclear {
    >&2 echo "\033[31m\033[01m[
    FATAL ERROR:
    ------------------
    $1 ]\033[0m"
    exit 1
}

function getProjectVersionFromPom {
    cat << EOF | xmllint --noent --shell pom.xml | grep content | cut -f2 -d=
setns pom=http://maven.apache.org/POM/4.0.0
xpath /pom:project/pom:version/text()
EOF
}

function generate_promotion_script {
    echo "Generating release promotion script 'promote-$version.sh'"
read -d '' script <<- EOF
#!/bin/bash
echo "Promoting release $version
Actions about to be performed:
------------------------------
\$(cat \$0 | tail -n +14)
------------------------------------------"
read -p "Press enter to continue or CTRL-C to abort"
# promote the source distribution by moving it from the staging area to the release area
# mv https://dist.apache.org/repos/dist/dev/incubator/dubbo/$version https://dist.apache.org/repos/dist/release/incubator/dubbo/ -m "Upload release to the mirrors"
#mvn org.sonatype.plugins:nexus-staging-maven-plugin:1.6.7:rc-release -DstagingRepositoryId=$stagingRepositoryId -DnexusUrl=https://oss.sonatype.org/ -DserverId=sonatype-nexus-staging -Ddescription="Release vote has passed"
# Renumber the next development iteration $next_version:
git checkout $branch
mvn release:update-versions --batch-mode
mvn versions:set versions:commit -DprocessAllModules=true -DnewVersion=$next_version
git add --all
git commit -m 'Start the next development version'
echo "
Please check the new versions and merge $branch to the base branch.
"
EOF

echo "$script" > promote-$version.sh
    chmod +x promote-$version.sh
    git add promote-$version.sh
}

function generate_rollback_script {
	echo "Generating release rollback script 'revert-$version.sh'"
read -d '' script <<- EOF
#!/bin/bash
echo -n "Reverting release $version
Actions about to be performed:
------------------------------
\$(cat \$0 | tail -n +14)
------------------------------------------
Press enter to continue or CTRL-C to abort"
read
# clean up local repository
git checkout $GIT_BRANCH
git branch -D $branch
git tag -d $tag
# clean up staging dist area
#svn rm https://dist.apache.org/repos/dist/dev/incubator/dubbo/$version -m "Release vote has failed"
# clean up staging maven repository
#mvn org.sonatype.plugins:nexus-staging-maven-plugin:1.6.7:rc-drop -DstagingRepositoryId=$stagingrepoid -DnexusUrl=https://oss.sonatype.org/ -DserverId=sonatype-nexus-staging -Ddescription="Release vote has failed"
# clean up remaining release files
find . -name "*.releaseBackup" -exec rm {} \\;
[ -f release.properties ] && rm release.properties
EOF
echo "$script" > revert-$version.sh

	chmod +x revert-$version.sh
	git add revert-$version.sh
}

function generate_release_vote_email {

    echo "Generating Vote email"

    echo "
    Hello Dubbo Community,

    This is a call for vote to release Apache Dubbo (Incubating) version $version.

    The release candidates (RC5):
    https://dist.apache.org/repos/dist/dev/incubator/dubbo/$version

    Git tag for the release (RC5):
    https://github.com/apache/incubator-dubbo/tree/dubbo-$version
    Hash for the release tag:
    3963d8fd93642398375ea92acb7ed4d2bc1b0518

    Release Notes:
    https://github.com/apache/incubator-dubbo/blob/dubbo-$version/CHANGES.md

    The artifacts have been signed with Key : 28681CB1, which can be found in the keys file:
    https://dist.apache.org/repos/dist/dev/incubator/dubbo/KEYS

    The vote will be open for at least 72 hours or until necessary number of votes are reached.

    Please vote accordingly:

    [ ] +1 approve
    [ ] +0 no opinion
    [ ] -1 disapprove with the reason

" | tail -n+2 > release-vote.txt

	git add release-vote.txt
}

hasFileChanged=`git status|grep -e "nothing to commit, working tree clean"|wc -l`
if [ $hasFileChanged -lt 1 ] ; then
    fail_noclear "ERROR: there are changes that have not committed in current branch ."
fi

if [ ! $( git config --get remote.staging.url ) ] ; then
    fail "
No staging remote git repository found. The staging repository is used to temporarily
publish the build artifacts during the voting process. Since no staging repository is
available at Apache, it is best to use a git mirror on your personal github account.
First fork the github Apache Dubbo (Incubating) mirror (https://github.com/apache/dubbo) and then
add the remote staging repository with the following command:
    $ git remote add staging git@github.com:<your personal github username>/dubbo.git
    $ git fetch staging
    $ git push staging
This will bring the staging area in sync with the origin and the release script can
push the build branch and the tag to the staging area.
"
fi


echo "Cleaning up any release artifacts that might linger: mvn -q release:clean"
mvn -q release:clean

# the branch on which the code base lives for this version
read -p 'Input the branch on which the code base lives for this version: ' GIT_BRANCH

version=$(getProjectVersionFromPom)
while [[ ! $version =~ ^[0-9]+\.[0-9]+\.[0-9]+(-M[0-9]+)?$ ]]
do
    read -p "Version to release (in pom now is $version): " -e t1
    if [ -n "$t1" ]; then
      version="$t1"
    fi
done

tag=dubbo-$version
branch=$version-release

major_version=$(expr $version : '\(.*\)\..*\..*')
minor_version=$(expr $version : '.*\.\(.*\)\..*')
bugfix_version=$(expr $version : '.*\..*\.\(.*\)')

next_version="$major_version.$minor_version.$(expr $bugfix_version + 1)-SNAPSHOT"
previous_minor_version=$(expr $bugfix_version - 1)
if [ $previous_minor_version -lt 0 ] ; then
    previous_version="$major_version.$minor_version.0-SNAPSHOT"
else
    previous_version="$major_version.$minor_version.$(expr $bugfix_version - 1)"
fi

log=../release.out

if [ -f $log ] ; then
    rm $log
fi

echo "Removing previous release tag $tag (if exists)"
oldtag=`git tag -l |grep -e "$tag"|wc -l` >> $log
[ "$oldtag" -ne 0 ] && git tag -d $tag >> $log

echo "Removing previous build branch $branch (if exists)"
oldbranch=`git branch |grep -e "$branch"|wc -l` >> $log
[ "$oldbranch" -ne 0 ] && git branch -D $branch >> $log

echo "Removing previous staging tag (if exists)"
git push origin :refs/tags/$tag >> $log

echo "Creating release branch"
releasebranch=`git branch -a |grep -e "origin/$branch$"|wc -l` >> $log
if [ $releasebranch -ne 0 ]
then
    echo "git checkout -b $branch origin/$branch"
    read -p "test"
    git checkout -b $branch origin/$branch >> $log
    if [ $? -ne 0 ] ; then
       fail_noclear "ERROR: git checkout -b $branch origin/$branch"
    fi
else
    echo "git checkout -b $branch origin/$GIT_BRANCH"
    read -p "test"
    git checkout -b $branch origin/$GIT_BRANCH >> $log
    if [ $? -ne 0 ] ; then
       fail_noclear "ERROR: git checkout -b $branch origin/$GIT_BRANCH"
    fi
fi

# Change version from SNAPSHOT to release ready
#mvn versions:set versions:commit -DprocessAllModules=true -DnewVersion=$version
# Add tag
#git tag -a dubbo-$version -m "generate tag dubbo-version" >> $log
#git push origin dubbo-$version >> $log
#mvn release:prepare -Darguments="-DskipTests" -DautoVersionSubmodules=true -Dusername=chickenlj -DupdateWorkingCopyVersions=true -DpushChanges=false -DdryRun=true
#if [ $? -ne 0 ] ; then
#    fail "ERROR: mvn release:prepare was not successful"
#fi
#mvn -Prelease release:perform  -Darguments="-DskipTests -Dmaven.deploy.skip=true" -DautoVersionSubmodules=true -Dusername=chickenlj -DdryRun=true
#if [ $? -ne 0 ] ; then
#   fail "ERROR: mvn release:perform was not successful"
#fi
mvn versions:set versions:commit -DprocessAllModules=true -DnewVersion=$version >> $log

# Determine the staging repository and close it after deploying the release to the staging area
stagingrepoid=$(mvn org.sonatype.plugins:nexus-staging-maven-plugin:1.6.7:rc-list -DnexusUrl=https://oss.sonatype.org/ -DserverId=sonatype-nexus-staging | grep -v "CLOSED" | grep -Eo "(comalibaba-\d+)";)

echo "Closing staging repository with id $stagingrepoid"
#mvn org.sonatype.plugins:nexus-staging-maven-plugin:1.6.7:rc-close -DstagingRepositoryId=$stagingrepoid -DnexusUrl=https://oss.sonatype.org/ -DserverId=sonatype-nexus-staging -Ddescription="Release has been built, awaiting vote"

read -p "test"
#mvn clean install -DskipTests
cd ./distribution
echo "Current dir: $(pwd)"
echo "Prepare for source and binary releases: mvn install -Prelease"
mvn install -Prelease >> $log
if [ $? -ne 0 ] ; then
   fail "ERROR: mvn clean install -Prelease"
fi
cd ./target
shasum -a 512 apache-dubbo-incubating-${version}-source-release.zip >> apache-dubbo-incubating-${version}-source-release.zip.sha512
shasum -a 512 apache-dubbo-incubating-${version}-bin-release.zip >> apache-dubbo-incubating-${version}-bin-release.zip.sha512

echo "Submit all release candidate packages to svn"
#svn mkdir https://dist.apache.org/repos/dist/dev/incubator/dubbo/$version-test -m "Create $version release staging area"
#svn co --force --depth=empty https://dist.apache.org/repos/dist/dev/incubator/dubbo/$version .
#svn add *
#svn commit -m "Upload dubbo-$version to staging area"

cd ../..
git add .
git commit -m "Prepare for release $version"
git push origin $branch

generate_promotion_script
generate_rollback_script
generate_release_vote_email
git checkout -b $branch-staging
git add .
git commit -m "Prepare for release $version"
git push staging $branch-staging