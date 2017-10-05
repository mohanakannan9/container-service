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

    function findLabel(key){
        return key.indexOf('identifier') > 0;
    }

    projectSearchLauncher.confirmTargets = function(targetList, config){
        // config contains information necessary to build the container launcher
        // Ask user to confirm the list of targets before building the container launch UI

        if (targetList.length) {

            var s = (targetList.length > 1) ? 's' : '';

            XNAT.dialog.open({
                title: 'Confirm Data To Run',
                width: 500,
                content: spawn('div.targetList.panel'),
                beforeShow: function(obj){
                    var inputArea = obj.$modal.find('.targetList');
                    inputArea.append(spawn('!',[
                        spawn('h3', targetList.length + ' '+config['root-element-name']+s+' queued for this container launch.'),
                        spawn('p','Select some or all to launch on, or add filters to your search table.')
                        ]));

                    targetList.forEach(function(target){
                        inputArea.append(
                            XNAT.ui.panel.input.checkbox({
                                label: target.label,
                                className: 'target',
                                value: target['accession-id']
                            })
                        )
                    });
                    inputArea.find('input[type=checkbox]').prop('checked','checked');
                },
                buttons: [
                    {
                        label: 'OK',
                        isDefault: true,
                        action: function(obj){
                            var targets = [];
                            obj.$modal.find('input.target').each(function(){
                                if ($(this).prop('checked')) targets.push($(this).val());
                            });
                            console.log(targets);
                        }
                    }
                ]
            });

        } else {

            xmodal.message('Error: no data selected. Cannot run container.');

        }
    };

    projectSearchLauncher.open = function(){
        // find obj in the config param of the passed object
        var obj = this.cfg.config.onclick.value.obj;

        XNAT.xhr.getJSON({
            url: XNAT.url.rootUrl('/data/search/'+obj['search-id']),
            success: function(data){
                console.log('found data');
                var targetList = [];
                if (data.ResultSet.Result.length){
                    data.ResultSet.Result.forEach(function(target){
                        // determine the label field -- it differs for each project and data type.
                        var labelField = Object.keys(target).find(findLabel);
                        targetList.push({ 'accession-id': target.key, 'label': target[labelField] });
                    });
                }

                projectSearchLauncher.confirmTargets(targetList, obj);
            }
        })
    };


}));