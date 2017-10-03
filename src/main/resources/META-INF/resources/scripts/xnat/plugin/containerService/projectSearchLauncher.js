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

    projectSearchLauncher.init = function(){
        $('.yui-pg-container').each(function(i){
            var dtTitleBar = $(this);
            var xsiType = dtTitleBar.prop('id').split('_')[0].substring(1);
            var availableCommands = [];
            XNAT.xhr.getJSON({
                    url: XNAT.url.rootUrl('/xapi/commands/available?project=${project.getId()}&xsiType='+xsiType)
                })
                .success(function(data){
                    var availableCommands = data;
                    if (!availableCommands.length) {
                        return false;
                    } else {
                        var opts = [];
                        availableCommands.forEach(function(command){
                            opts.push(spawn('option',{ value: command['wrapper-id'] }, command['wrapper-description']))
                        });
                        dtTitleBar.find('tr').first().append(spawn('td',[
                            spawn('input#cs-run-'+i+'|type=submit'),
                            spawn('select#cs-select-'+i, opts )
                            ]));

                        var csRunButton = new YAHOO.widget.Button('cs-run-'+i, {
                            type: 'menu',
                            menu: 'cs-select-' + i,
                            label: 'Run Containers',
                            submenualignment: ["tr", "br"]
                        });
                    }
                });
        })
    };

}));