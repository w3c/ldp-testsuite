###############################################################################
##### Moving generated reports to needed locations
###############################################################################

The generated output files: 
  ldp.html
  ldp.ttl
  ldp.jsonld
  ldp-earl-manifest*.*
get committed to the W3C Mercurial repo at:
   https://dvcs.w3.org/hg/ldpwg/file/default/tests/reports
   
The generated output files:
  ldp-testsuite-coverage-report.html
gets committed to the gh-pages branch as
  manifest/index.html  
  
###############################################################################
#####   Client-only tests  
###############################################################################
Client-only tests are maintained solely in an EARL turtle file.  There is no
reason to put these in Java source annotations and then generate the EARL file.
See ldp-earl-manifest-client-only.ttl

###############################################################################
######  Generating WG summary report
###############################################################################

To generate the ldp.html WG report needed, you can simply do the following:
$earl-report -f html -o ldp.html --name "LDP" --bibRef "[[LDP]]" 
  --template ldp-template.html.haml --manifest ldp-earl-manifest.ttl 
  (gh-pages-branch)/contrib/*.ttl 

Just need json?
$earl-report -f json -o ldp.jsonld --manifest ldp-earl-manifest.ttl contrib/*.ttl 

Just need turtle?
$earl-report -f ttl -o ldp.ttl --manifest ldp-earl-manifest.ttl contrib/*.ttl 

Already have json but need html?
$earl-report -f html --json ldp.jsonld --template ldp-template.html.haml -o ldp.html 

