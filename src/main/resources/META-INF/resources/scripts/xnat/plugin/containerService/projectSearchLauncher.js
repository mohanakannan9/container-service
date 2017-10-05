console.log('containerServices-projectSearchLauncher.js');

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
    var projectSearchLauncher;

    XNAT.plugin.containerService.projectSearchLauncher = projectSearchLauncher =
        getObject(XNAT.plugin.containerService.projectSearchLauncher || {});

    /*
     * To create a bulk container launch from a data search table:
     * 1. Get the JSON list of XNAT data IDs to target as your root element, and the root element name
     * 1a. The launcher will be provided with an XNAT search ID to query to get that list
     * 2. Submit the target list to the bulklaunch API for that command & wrapper
     * 3. Figure out the rest of the parameters and inputs for launching using the same logic as the scan bulk launcher
     */

    projectSearchLauncher.open = function(){
        // find obj in the config param of the passed object
        var obj = this.cfg.config.onclick.value.obj;

        XNAT.xhr.getJSON({
            url: XNAT.url.rootUrl('/data/search/'+obj['search-id']),
            success: function(data){
                var targetList = [];
                if (data.ResultSet.Result.length){
                    data.ResultSet.Result.forEach(function(target){
                        targetList.push(target.key);
                    })
                }
                console.log(obj, targetList);
            }
        })
    }
}));