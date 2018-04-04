#!/bin/bash
[[ $# -ne 1 ]] && echo Usage: $0 [CSV_FN] && exit -1

CSV_FN=$1
HTML_FN="$CSV_FN.html"

echo "<table>" > $HTML_FN
head -n 1 $CSV_FN | \
    sed -e 's/^/<tr><th>/' -e 's/,/<\/th><th>/g' -e 's/$/<\/th><\/tr>/' >> $HTML_FN
tail -n +2 $CSV_FN | \
    sed -e 's/^/<tr><td>/' -e 's/,/<\/td><td>/g' -e 's/$/<\/td><\/tr>/' >> $HTML_FN
echo "</table>" >> $HTML_FN

xdg-open $HTML_FN
