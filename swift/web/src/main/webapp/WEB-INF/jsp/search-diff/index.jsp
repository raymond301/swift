<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="/common/bootstrap/css/bootstrap.min.css" rel="stylesheet" media="screen">
    <style type="text/css">
        body {
            margin: 15px;
        }

        table#fileTable, table#searchSettingsTable {
            width: auto;
        }

        #fileTable td.present {
            background-color: #ebffee;
        }

        #fileTable tr:hover td.present {
            background-color: #e3f6e6;
        }

        #fileTable thead tr th {
            width: 2em;
            height: 18em;
            overflow-y: hidden;
        }

        #fileTable thead tr th .filename {
            width: 0;
            height: 0;
            font-size: 12px;
            /* FF Chrome Opera etc */
            -webkit-transform: rotate(-90deg);
            -moz-transform: rotate(-90deg);
            -o-transform: rotate(-90deg);
            /* IE */
            filter: progid:DXImageTransform.Microsoft.BasicImage(rotation=1);
        }

        #searchSettingsTable tr th, #searchSettingsTable tr td {
            font-size: 12px;
        }

        .edit-link, .qa-link {
            display: block;
            color: #fff;
            font-size: 1px;
            line-height: 100px;
            overflow: hidden;
            width: 20px;
            height: 20px;
            background-repeat: no-repeat;
            background-position: 0 0;
            float: left;
        }

        .edit-link {
            background-image: url(/report/search_edit.gif);
        }

        .qa-link {
            background-image: url(/report/search.gif);
        }


    </style>
    <!-- HTML5 shim, for IE6-8 support of HTML5 elements -->
    <!--[if lt IE 9]>
    <script src="/common/bootstrap/js/html5shiv.js"></script>
    <![endif]-->

    <script type="text/javascript" src="/common/bootstrap/js/jquery_1.9.0.min.js"></script>
    <title>Search Difference | ${title}</title>
    <script type="text/javascript">
        // Read a page's GET URL variables and return them as an associative array.
        function getUrlVars() {
            var vars = [], hash;
            var hashes = window.location.href.slice(window.location.href.indexOf('?') + 1).split('&');
            for (var i = 0; i < hashes.length; i++) {
                hash = hashes[i].split('=');
                vars.push(hash[0]);
                vars[hash[0]] = hash[1];
            }
            return vars;
        }

        function appendSearchTitle(row, search) {
            qaLink = "";
            if(search.qaUrl) {
                qaLink = '<a href="'+search.qaUrl+'" class="qa-link"></a>';
            }
            row.append('<th>' + search.title +
                    '<a href="/start/?load='+search.id+'" class="edit-link"></a>'+
                    qaLink+'</th>');
        }

        // Convert the data, filling the #fileTable object
        function fillFileTable(data) {
            // Head row - list of all files
            head = $("#fileTable > thead");
            head.empty();
            head.append("<tr></tr>");
            headRow = $("#fileTable > thead > tr");
            headRow.append('<td></td>');

            $(data.diffs.inputFiles).each(function (index, file) {
                headRow.append('<th><div class="filename" title="' + file.name + '">' + file.name + '</div></th>');
            });

            // Body - for each search, protein group counts + extra
            body = $("#fileTable > tbody");
            body.innerHTML = "";
            $(data.diffs.searches).each(function (searchIndex, search) {
                row = $("<tr></tr>");
                appendSearchTitle(row, search);

                $(data.diffs.inputFiles).each(function (index, file) {
                    cell = data.diffs.results[file.id][search.id];
                    if (cell.present) {
                        row.append('<td style="text-align: center" class="present">' + cell.proteinGroups + '</td>');
                    } else {
                        row.append('<td></td>');
                    }
                });
                body.append(row);
            });
        }

        function dateToYMD(date) {
            var d = date.getDate();
            var m = date.getMonth() + 1;
            var y = date.getFullYear();
            return '' + y + '-' + (m<=9 ? '0' + m : m) + '-' + (d <= 9 ? '0' + d : d);
        }

        function fillSearchSettingsTable(data) {
            // Head row - list of all files
            head = $("#searchSettingsTable > thead");
            head.empty();
            head.append("<tr></tr>");
            headRow = $("#searchSettingsTable > thead > tr");
            headRow.append('<td></td>' +
                    '<th>Start Date</th>' +
                    '<th>Engines</th>' +
                    '<th>Database</th>' +
                    '<th>Protease</th>' +
                    '<th>Missed Cleavages</th>' +
                    '<th>Fixed Mods</th>' +
                    '<th>Variable Mods</th>' +
                    '<th>Peptide Tolerance</th>' +
                    '<th>Fragment Tolerance</th>' +
                    '<th>Instrument</th>' +
                    '<th>Raw2Mgf</th>' +
                    '<th>Protein Prob.</th>' +
                    '<th>Min Peptides</th>' +
                    '<th>Peptide Prob.</th>' +
                    '<th>NTT</th>',
                    '<th>Independent Samples</th>',
                    '<th>Protein Families</th>');

            // Body - for each search, protein group counts + extra
            body = $("#searchSettingsTable > tbody");
            body.innerHTML = "";

            $(data.diffs.searches).each(function (searchIndex, search) {
                row = $("<tr></tr>");
                appendSearchTitle(row, search);
                row.append('<td>' + dateToYMD(new Date(search.startTimestamp)) + '</td>');
                row.append('<td>' + search.engines.sort().join("<br/>") + '</td>');
                row.append('<td>' + search.curationName + '</td>');
                row.append('<td>' + search.enzymeName + '</td>');
                row.append('<td>' + search.missedCleavages + '</td>');
                row.append('<td>' + search.fixedModifications.sort().join("<br/>") + '</td>');
                row.append('<td>' + search.variableModifications.sort().join("<br/>") + '</td>');
                row.append('<td>' + search.peptideTolerance + '</td>');
                row.append('<td>' + search.fragmentTolerance + '</td>');
                row.append('<td>' + search.instrument + '</td>');
                row.append('<td>' + search.raw2mgfConvertor + '</td>');
                row.append('<td>' + search.scaffoldProteinProbability + '</td>');
                row.append('<td>' + search.scaffoldMinimumPeptideCount + '</td>');
                row.append('<td>' + search.scaffoldPeptideProbability + '</td>');
                row.append('<td>' + search.scaffoldMinimumNonTrypticTerminii + '</td>');
                row.append('<td>' + search.useIndependentSampleGrouping + '</td>');
                row.append('<td>' + search.useFamilyProteinGrouping + '</td>');
                body.append(row);
            });
        }

        $(document).ready(function () {
            id = getUrlVars()['id'];
            $.get("/service/search-diffs.json",
                    {'id': id},
                    function (data) {
                        fillFileTable(data);
                        fillSearchSettingsTable(data);
                    },
                    "json");
        });
    </script>
</head>
<body>
<h2>Search Comparison</h2>
<p>The number is the amount of protein groups per given search coming from given file.</p>
<br clear="all"/>

<table id="fileTable" class="table table-bordered table-condensed table-hover" style="float:left">
    <thead>
    <tr>
        <td><img src="/start/images/ajax-loader.gif"/></td>
    </tr>
    </thead>
    <tbody></tbody>
    <tfoot></tfoot>
</table>
<br clear="all"/>

<h2>Search Settings</h2>
<table id="searchSettingsTable" class="table table-bordered table-condensed table-hover" style="float:left">
    <thead>
    <tr>
        <td><img src="/start/images/ajax-loader.gif"/></td>
    </tr>
    </thead>
    <tbody></tbody>
    <tfoot></tfoot>
</table>
<br clear="all"/>
</body>
</html>