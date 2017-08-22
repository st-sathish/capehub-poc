
var chWorkflow = chWorkflow || {};

chWorkflow.init = function(selectElm, configContainer, tags) {
  chWorkflow.container = configContainer;
  chWorkflow.selector = selectElm;
  $(chWorkflow.selector).change( function() {
    chWorkflow.definitionSelected($(this).val(), configContainer);
  });
  chWorkflow.loadDefinitions(selectElm, configContainer, tags);
}

chWorkflow.loadDefinitions = function(selector, container, tags) {
  $.ajax({
    async: false,
    method: 'GET',
    url: '/workflow/definitions.json',
    dataType: 'json',
    success: function(data) {
      var wfDefinitions = chUtils.ensureArray(data.definitions.definition);
      outerloop:
      for (i in wfDefinitions) {
        if (wfDefinitions[i].id == 'error')
        	continue;
        
        if(tags != undefined && $.isArray(tags)) {
        	var definitionTags = chUtils.ensureArray(wfDefinitions[i].tags.tag);
        	for(y in tags) {
        		var include = _.contains(definitionTags, tags[y]);
        		if(!include) continue outerloop;
        	}
        }
        
        var option = document.createElement("option");
        option.setAttribute("value", wfDefinitions[i].id);
        option.innerHTML = wfDefinitions[i].title || wfDefinitions[i].id;
        if (wfDefinitions[i].id == "notify") {
        	option.setAttribute("selected", "true");
        }
        $(selector).append(option);
      }
      chWorkflow.definitionSelected($(selector).val(), container);
    }
  });
}

chWorkflow.definitionSelected = function(defId, container, callback) {
  if(typeof chWorkflowPanel != 'undefined')
    chWorkflowPanel = null;
  $(container).load('/workflow/configurationPanel?definitionId=' + defId,
    function() {
      $(container).show('fast');
      if (callback) {
        callback();
      }
      if(chWorkflowPanel && chWorkflowPanel.registerComponents){
    	//Clear the previously selected panel's components
      }else{
        chUtils.log("component registration handler not found in workflow.", defId);
      }
    }
  );
}

chWorkflow.getConfiguration = function(container) {
  var out = new Object();
  $(container).find('.configField').each( function(idx, elm) {
    if ($(elm).is('[type=checkbox]')) {
      if ($(elm).is(':checked')) {
        out[$(elm).attr('id')] = $(elm).val();
      }
    } else {
      out[$(elm).attr('id')] = $(elm).val();
    }
  });
  return out;
}

