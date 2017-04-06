/*
 * web: commandHistory.js
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

/*!
 * Flexible script to be used in the UI to launch
 */

console.log('commandHistory.js');

var XNAT = getObject(XNAT || {});

(function(factory){
    if (typeof define === 'function' && define.amd) {
        define(factory);
    }
    else if (typeof exports === 'object') {
        module.exports = factory();
    }
    else {
        return factory();
    }
}(function(){
    // populate history table
    var containerService,
        historyTable,
        historyTableContainer = $('#container-history-container'),
        undefined,
        rootUrl = XNAT.url.rootUrl;

    XNAT.containerService = containerService =
        getObject(XNAT.containerService || {});

    XNAT.containerService.historyTable = historyTable =
        getObject(XNAT.containerService || {});

    function errorHandler(e){
        console.log(e);
        xmodal.alert({
            title: 'Error',
            content: '<p><strong>Error ' + e.status + ': '+ e.statusText+'</strong></p><p>' + e.responseText + '</p>',
            okAction: function () {
                xmodal.closeAll();
            }
        });
    }

    function getCommandHistory(appended){
        appended = (appended) ? '?'+appended : '';
        return rootUrl('/xapi/containers' + appended);
    }

    historyTable.table = function(callback){
        // initialize the table - we'll add to it below
        var chTable = XNAT.table({
            className: 'sitewide-command-configs xnat-table',
            style: {
                width: '100%',
                marginTop: '15px',
                marginBottom: '15px'
            }
        });

        // add table header row
        chTable.tr()
            .th({ addClass: 'left', html: '<b>ID</b>' })
            .th('<b>Image</b>')
            .th('<b>Command</b>')
            .th('<b>User</b>')
            .th('<b>Date</b>')
            .th('<b>Input</b>')
            .th('<b>Output</b>');

        function displayDate(timestamp){
            var d = new Date(timestamp);
            return d.toISOString().replace('T',' ').replace('Z',' ');
        }

        function displayInput(inputObj){
            for (var i in inputObj){
                if (i=="scan") {
                    var sessionId = inputObj[i].split('/')[2];
                    var scanId = inputObj[i].split('/scans/')[1];
                    return spawn(['a|href='+rootUrl('/data/experiments/'+sessionId), sessionId+': '+scanId ]);
                }
            }
        }

        function displayOutput(outputArray){
            var o = outputArray[0];
            return o.label;
        }

        XNAT.xhr.getJSON({
            url: getCommandHistory(),
            fail: function(e){
                errorHandler(e);
            },
            success: function(data){
                if (data.length > 0) {
                    data.forEach(function(item){
                        chTable.tr({title: item['container-id'], id: item['container-id'] })
                            .td({ addClass: 'left', html: '<b>'+item['id']+'</b>' })
                            .td(item['docker-image'])
                            .td('dcm2niix-scan')
                            .td(item['user-id'])
                            .td([ displayDate(item['timestamp']) ])
                            .td([ displayInput(item['rawInputs']) ])
                            .td([ displayOutput(item['outputs']) ]);
                    })
                } else {
                    chTable.tr()
                        .td({ colspan: 7, html: "No history entries found" });
                }

            }
        });

        historyTable.$table = $(chTable.table);

        return chTable.table;
    };

    historyTable.init = function(container){
        var manager = $$(container || historyTableContainer);
        manager.html('');

        manager.append(historyTable.table());
    };

    historyTable.init();

}));