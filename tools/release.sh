#!/usr/bin/env bash

scripts/release
mkdir tmp
cp release/kag_server_status.js tmp/kag_server_status.js
git checkout gh-pages
cp tmp/kag_server_status.js out/kag_server_status.js
rm -r style
git checkout master -- style
rm index.html
git checkout master -- index.html
rm -r tmp