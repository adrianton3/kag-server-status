#!/usr/bin/env bash

scripts/release
cp release/kag_server_status.js out/kag_server_status.js
git checkout gh-pages
rm -rf style
git checkout master -- style
rm index.html
git checkout master -- index.html