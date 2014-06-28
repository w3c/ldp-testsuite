#!/bin/sh -v
earl-report -f json -o ldp.jsonld --manifest ldp-earl-manifest.ttl contrib/*.ttl 
earl-report -f html --json ldp.jsonld --template ldp-template.html.haml -o ldp.html 
