# Updates the verison in gradle file

Then uses Github Action to commit changes to repo https://github.com/EndBug/add-and-commit 

#!/bin/bash

set -o errexit # Exit if any command fails
set -o nounset # Exit if script tries to use unset variable

commandName=$(basename $0)

# Use 'realpath' because 'readlink -m' does not work on MacOS.
# Install 'realpath' in MacOS via "brew install coreutils"
scriptPath=$(realpath -m "$(dirname $0)" )

printUsageAndExit() {
  cat << USAGE >&2
  This script increments the API product_version parameter in the given versions.gradle file.
  Increment can be in MAJOR/MINOR/PATCH component of the semantic version.
  Also it's possible to generate new version as a SNAPSHOT or BUILD. With BUILD, a timestamp with minute resolution will be appended, so the same version can be used.
  Usage: ${commandName} -f <versions.gradle file> -v <version_component> -s <release type>
    -f : Path to versions.gradle file that contains product_version line.
    -v : Version component to increment. Also sub-components will be set to 0. i.e. 1.1.1 major increment will result 2.0.0
    -q : Quiet
    -V : Verbose
 snapshot: generate a snapshot version, i.e. x.y.z-SNAPSHOT
 build: Generate a timestamped build version, i.e. x.y.z-BUILD-yymmdd_HHMM
 release: Generate a release version
USAGE
  exit 1
}

function validateParameters() {
  if [ -z "${VERSION_FILE}" ] || [ -z "${VERSION_INCREMENT_COMPONENT}" ] || [ -z "${VERSION_SNAPSHOT_MODE}" ]; then
    printUsageAndExit
  fi
}

# Prints given parameter to std out only if quiet is not set.
function log_info() {
  if [[ $QUIET -ne 1 ]]; then  echo -e "$(date +"%Y_%m_%d %H:%M:%S") $@";  fi
}

# Prints given parameter to std out only if verbose is set.
function log_verbose() {
  if [[ $VERBOSE -eq 1 ]]; then  echo -e "$(date +"%Y_%m_%d %H:%M:%S") $@";  fi
}

# Prints given parameter to both std out/err
function log_error() {
  echo -e "$(date +"%Y_%m_%d %H:%M:%S") $@" 1>&2
}

function semverParseInto() {
    local RE='[^0-9]*\([0-9]*\)[.]\([0-9]*\)[.]\([0-9]*\)\([0-9A-Za-z-]*\)'

    #MAJOR
    eval $2=`echo $1 | sed -e "s#${RE}#\1#"`

    #MINOR
    eval $3=`echo $1 | sed -e "s#${RE}#\2#"`

    #PATCH
    eval $4=`echo $1 | sed -e "s#${RE}#\3#"`

    #SNAPSHOT
    eval $5=`echo $1 | sed -e "s#${RE}#\4#"`
}

function getCurrentVersion() {

  local RE=$'\(^[[:blank:]]*product_version[[:blank:]]*=[[:blank:]]*["\']*\)\([^"\']*\)\(["\']*[[:blank:]]*$\)'

  VERSION_FILE="${1}"

  log_info "Loading product version from '${VERSION_FILE}'"
  CURRENT_VERSION=$(grep 'product_version' "${VERSION_FILE}" | head -1 | sed "s#${RE}#\2#" )
  log_info "Current Version : '${CURRENT_VERSION}'"

  eval $2="${CURRENT_VERSION}"
}

function saveNewVersion() {
  local RE=$'\(^[[:blank:]]*product_version[[:blank:]]*=[[:blank:]]*["\']*\)\([^"\']*\)\(["\']*[[:blank:]]*$\)'

  VERSION_FILE="${1}"
  NEW_VERSION="${2}"

  sed -ibak "s#${RE}#\1${NEW_VERSION}\3#" "${VERSION_FILE}"
}

function increaseVersion() {
  CURRENT_VERSION=${1}
  INCREMENT_COMPONENT=${2}
  SNAPSHOT_MODE=${3}

  MAJOR=0
  MINOR=0
  PATCH=0
  SNAPSHOT=""


  semverParseInto ${CURRENT_VERSION} MAJOR MINOR PATCH SNAPSHOT

  case ${INCREMENT_COMPONENT} in
   "major") log_info "Updating major version"; MAJOR=$((MAJOR+1)); MINOR=0; PATCH=0;;
   "minor") log_info "Updating minor version"; MINOR=$((MINOR+1)); PATCH=0;;
   "patch") log_info "Updating patch version"; PATCH=$((PATCH+1));;
   "none") log_info "Not updating any version component.";;
  esac

  case ${SNAPSHOT_MODE} in
    "snapshot") SNAPSHOT="-SNAPSHOT";;
    "build") SNAPSHOT="-BUILD-$(date "+%y%m%d-%H%M")";;
    "release") SNAPSHOT="";;
  esac

  eval $4=`echo "${MAJOR}.${MINOR}.${PATCH}${SNAPSHOT}"`

}


# #######################################################################################
# Main
# #######################################################################################

# Process command line parameters
while getopts "f:v:s:Vq" opt
do
  case "${opt}" in
    f) VERSION_FILE="${OPTARG}";;
    v) VERSION_INCREMENT_COMPONENT="${OPTARG}";;
    s) VERSION_SNAPSHOT_MODE="${OPTARG}";;
    V) VERBOSE=1; QUIET=0;;
    q) QUIET=1; VERBOSE=0;;
  esac
done

QUIET=${QUIET:-}
VERBOSE=${VERBOSE:-}
VERSION_FILE=${VERSION_FILE:-}
VERSION_SNAPSHOT_MODE=${VERSION_SNAPSHOT_MODE:-}
VERSION_INCREMENT_COMPONENT=${VERSION_INCREMENT_COMPONENT:-}

validateParameters

CURRENT_VERSION=""
getCurrentVersion "${VERSION_FILE}" CURRENT_VERSION


NEW_VERSION=""
increaseVersion ${CURRENT_VERSION} ${VERSION_INCREMENT_COMPONENT} ${VERSION_SNAPSHOT_MODE} NEW_VERSION

log_info "New Version : '${NEW_VERSION}'"

saveNewVersion  "${VERSION_FILE}" "${NEW_VERSION}"
