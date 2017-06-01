#!/bin/bash

# This script generates the release notes every time a release of NAR is made.
#
# Usage example:
# sh release-notes.sh 3.5.0

# -- Functions --

githubIssue() {
	page=$(curl -fsL "https://github.com/maven-nar/nar-maven-plugin/issues/$1")
	if echo "$page" | grep -q 'State State--\(red\|purple\)'
	then
		# Issue is closed or merged.
		echo "$page" | grep -A 1 'js-issue-title' | tail -n1 | sed 's/^ *//'
	else
		# Issue was not fixed!
		echo
	fi
}

# -- Main --

v=$1
if [ -z "$v" ]
then
  echo "Usage: sh release-notes.sh <new-version>"
  exit 1
fi

git fetch --tags
p=$(git tag -l | grep -B 1 "nar-maven-plugin-$v" | head -n1 | sed 's/.*nar-maven-plugin-//')
if [ -z "$p" ]
then
  echo "Invalid version: $v"
  exit 2
fi
echo "Version  = $v"
echo "Previous = $p"

echo
echo "--------------------------------"
echo

echo "It is a great honor to announce a new version of the NAR plugin for Maven."

echo 
echo "nar-maven-plugin-$v:"
echo "======================="
echo "$(git log -1 "nar-maven-plugin-$v"  --pretty=format:"%ad")"

echo
echo "authors with commit counts"
echo "--------------------------"
echo 
echo "Author | Count"
echo "------:|:-----"
git shortlog -ns "nar-maven-plugin-$p..nar-maven-plugin-$v" | while read line
do
	commits=${line%	*}
	author=${line#*	}
	echo "$author | $commits"
done

echo
echo "list of commits"
echo "--------------------"
echo
echo "Author | Commit"
echo "------:|:------"
git log "nar-maven-plugin-$p..nar-maven-plugin-$v" --pretty=format:"%an | %s" | grep -v 'Bump to next development cycle' | grep -v 'prepare release' | grep -v 'Merge pull request'
 
echo
echo "issues fixed"
echo "---------------"
echo
echo "Issue | Title"
echo "-----:|:-----"
git log "nar-maven-plugin-$p..nar-maven-plugin-$v" --pretty=format:"%an|%s|%B" | grep '#[0-9][0-9]*' | sed 's/.*#\([0-9][0-9]*\).*/\1/' | sort -nu | while read issue
do
	title=$(githubIssue "$issue")
	test "$title" && echo "#$issue | $title"
done
