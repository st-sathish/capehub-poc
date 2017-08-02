var ocUpload = (function() {

  this.DEFAULT_WORKFLOW_DEFINITION = 'full';
  this.WORKFLOW_DEFINITION_URL = '/workflow/definitions.json';
  this.WORKFLOW_PANEL_URL = '/workflow/configurationPanel?definitionId=';
  this.SERIES_SEARCH_URL = '/series/series.json';
  this.SERIES_URL = '/series'
  this.INGEST_CREATE_MP_URL = '/ingest/createMediaPackage';
  this.INGEST_ADD_CATALOG_URL = '/ingest/addDCCatalog';
  this.INGEST_ADD_TRACK = '/ingest/addTrack';
  this.INGEST_PROGRESS_URL = '/upload/job';
  this.INGEST_START_URL = '/ingest/ingest';
  this.CREATE_UPLOAD_JOB = '/upload/newjob';
  this.UPLOAD_CHUNK_URL = '/upload/job/';
  this.UPLOAD_GET_PAYLOAD = '/upload/job/';
  this.UPLOAD_COMPLETE = 'COMPLETE';
  this.UPLOAD_MEDIAPACKAGE = '/upload/mediapackage/';
  this.UPLOAD_PROGRESS_INTERVAL = 2000;
  this.CHUNKSIZE = 1024 * 1024 * 100;
  this.INFO_URL = "/info/me.json";
  this.KILOBYTE = 1024;
  this.MEGABYTE = 1024 * 1024;
  this.ANOYMOUS_URL = "/info/me.json";
  this.CREATE_NEW_JOB_URL = "/upload/newjob";
  this.UPLOAD_URL = "/upload/job/";

  /** $(document).ready()
   *
   */
  this.init = function() {
    ocUtils.log('Initializing UI');
    $('#addHeader').jqotesubtpl('templates/upload.tpl', {});
    $('#processingRecording').jqotesubtpl('templates/processing-instructions.tpl', {});	
    $('#common-data').jqotesubtpl('templates/common-data.tpl', {});
    $('#additional-description').jqotesubtpl('templates/additional-description.tpl', {});

    $('.unfoldable-header').click(ocUpload.UI.toggleUnfoldable);
    $('.dc-metadata-field').change(ocUpload.UI.formFieldChanged);
    $('.uploadtype-select').click(ocUpload.UI.selectUploadType);
    $('.file-source-select').click(ocUpload.UI.selectFileSource);
    $('.flavor-presentation-checkbox').change(ocUpload.UI.selectFlavor);
    $('#workflowSelector').change(ocUpload.UI.selectWorkflowDefinition);
    $('#submitButton').button().click(startUpload);
    $('#cancelButton').click(backToRecordings);

    var initializerDate;
    initializerDate = new Date();

    $('#startTimeHour').val(initializerDate.getHours());
    $('#startTimeMin').val(initializerDate.getMinutes());

    $('#recordDate').datepicker({
      showOn: 'both',
      buttonImage: '/admin/img/icons/calendar.gif',
      buttonImageOnly: true,
      dateFormat: 'yy-mm-dd'
    });
    $('#recordDate').datepicker('setDate', initializerDate);

    ocWorkflow.init($('#workflowSelector'), $('.workflowConfigContainer'), ['upload']);
    
    initSeriesAutocomplete();
    
    $.ajax({
      url: ocUpload.INFO_URL,
      dataType: 'json',
      success: function(data) {
        if(data.org.properties['adminui.chunksize'] != undefined) {
          ocUpload.CHUNKSIZE = data.org.properties['adminui.chunksize'] * 1024;
        }
        
      }
    });
    
  }

  function initSeriesAutocomplete() {
    ocUtils.log('Initializing autocomplete for series field')
    $('#seriesSelect').autocomplete({
      source: function(request, response) {
        $.ajax({
          url: ocUpload.SERIES_SEARCH_URL + '?seriesTitle=' + request.term + '&edit=true',
          dataType: 'json',
          type: 'GET',
          success: function(data) {
            data = data.catalogs;
            var series_list = [];
            $.each(data, function(){
              series_list.push({
                value: this['http://purl.org/dc/terms/']['title'][0].value,
                id: this['http://purl.org/dc/terms/']['identifier'][0].value
              });
            });
            series_list.sort(function stringComparison(a, b)	{
              a = a.value;
              a = a.toLowerCase();
              a = a.replace(/ä/g,"a");
              a = a.replace(/ö/g,"o");
              a = a.replace(/ü/g,"u");
              a = a.replace(/ß/g,"s");

              b = b.value;
              b = b.toLowerCase();
              b = b.replace(/ä/g,"a");
              b = b.replace(/ö/g,"o");
              b = b.replace(/ü/g,"u");
              b = b.replace(/ß/g,"s");

              return(a==b)?0:(a>b)?1:-1;
            });
            response(series_list);
          },
          error: function() {
            ocUtils.log('could not retrieve series_data');
          }
        });
      },
      select: function(event, ui){
        $('#isPartOf').val(ui.item.id);
      },
      change: function(event, ui){
        if($('#isPartOf').val() === '' && $('#seriesSelect').val() !== ''){
          ocUtils.log("Searching for series in series endpoint");
          $.ajax({
            url : ocUpload.SERIES_SEARCH_URL,
            type : 'get',
            dataType : 'json',
            success : function(data) {
              var DUBLIN_CORE_NS_URI  = 'http://purl.org/dc/terms/',
              series_input = $('#seriesSelect').val(),
              series_list = data["catalogs"],
              series_title,
              series_id;
              $('#isPartOf').val('');
              for (i in series_list) {
                var series_title, series_id;
                series_title = series_list[i][DUBLIN_CORE_NS_URI]["title"] ? series_list[i][DUBLIN_CORE_NS_URI]["title"][0].value : "";
                series_id = series_list[i][DUBLIN_CORE_NS_URI]["identifier"] ? series_list[i][DUBLIN_CORE_NS_URI]["identifier"][0].value : "";
                if (series_title === series_input){
                  $('#isPartOf').val(series_id);
                  break;
                }
              }
            }
          });
        }
      },
      search: function(){
        $('#isPartOf').val('');
      }
    });
  }

  this.checkRequiredFields = function() {
    ocUtils.log('Checking for missing inputs');
    var missing = [];

    if ($.trim($('#title').val()) == '') {
      ocUtils.log('Missing input: title');
      missing.push('title');
    }

    if ($.trim($('#recordDate').val()) == '') {
      ocUtils.log('Missing input: recordDate');
      missing.push('recordDate');
    }

    var fileSelected = false;
    $('.uploadForm-container:visible').each(function() {
      var file = $(this).contents().find('.file-selector').val();
      if (file !== undefined && file !== '') {
        fileSelected |= true;
      }
    });
    if (!fileSelected) {
      ocUtils.log('Missing input: no file selected');
      missing.push('track');
    }

    return missing.length > 0 ? missing : false;
  }

  function startUpload() {
    var missingFields = ocUpload.checkRequiredFields();
    ocUpload.UI.collectFormData();

    if (missingFields === false) {
      ocUpload.Ingest.begin();
    } else {
      ocUpload.UI.showMissingFieldsNotification();
      updateMissingFieldNotification(missingFields);
    }
  }

  this.backToRecordings = function() {
    location.href = "/admin/index.html#/recordings?" + window.location.hash.split('?')[1];
  }

  return this;
})();


/** @namespace UI functions
 *
 */
ocUpload.UI = (function() {

  /**
   * collected form data
   */
  var metadata = new Array();

  this.showMissingFieldsNotification = function() {
    $('#missingFieldsContainer').show();
  }

  this.updateMissingFieldNotification = function(missingFields) {
    var errorContainer = $('.scheduler-info-container');

    ocUtils.log('Updating missing fields notification');
    if (missingFields == false) {
      $('#missingFieldsContainer').hide();
      $('.label-error').removeClass('label-error');
    } else {
      $('#missingFieldsContainer').find('.missing-fields-item').each(function() {
        var fieldname = $(this).attr('id').substr(5);
        if ($.inArray(fieldname, missingFields) != -1) {
          $(this).show();
        } else {
          $(this).hide();
        }
      });

      for(var i in missingFields ) {
        $('#' + missingFields[i] +"Label").addClass('label-error');
      }

      $(window).scrollTop(errorContainer.offset().top)
             .scrollLeft(errorContainer.offset().left);
    }
  }

  this.toggleUnfoldable = function() {
    $(this).next('.unfoldable-content').toggle();
    $(this).find('.unfoldable-icon')
    .toggleClass('ui-icon-triangle-1-e')
    .toggleClass('ui-icon-triangle-1-s');
  }

  this.formFieldChanged = function() {
    ocUpload.UI.updateMissingFieldNotification(ocUpload.checkRequiredFields());
  }

  this.selectUploadType = function() {
    var $toHide = [];
    var $toShow = [];
    if ($(this).hasClass('uploadType-single')) {
      $toShow = $('#uploadContainerSingle');
      $toHide = $('#uploadContainerMulti');
    } else if ($(this).hasClass('uploadType-multi')) {
      $toShow = $('#uploadContainerMulti');
      $toHide = $('#uploadContainerSingle');
    }
    $toHide.hide();
    $toShow.show();
  }

  this.selectFileSource = function() {
    var location = $(this).val();
    var $container = $(this).parent().next('li').find('iframe');
    if(location == "local") {
      $container.attr('src', 'upload.html');
    } else {
      $container.attr('src', '../ingest/filechooser-' + location + '.html');
    }
  }

  this.selectFlavor = function() {
    var $flavorField = $(this).parent().prev().find('.track-flavor');
    if ($(this).is(':checked')) {
      $flavorField.val('presentation/source');
    } else {
      $flavorField.val('presenter/source');
    }
  }

  this.selectWorkflowDefinition = function() {
    var defId = $(this).val();
    var $container = $(this).parent().next('.workflowConfigContainer');
    $container.load(ocUpload.WORKFLOW_PANEL_URL + defId);
  }

  this.showProgressDialog = function() {
    $('#grayOut').css('display','block');
    $('#progressStage').dialog(
    {
      modal: true,
      width: 450,
      height: 'auto',
      position: ['center', 'center'],
      title: 'Uploading File',
      create: function (event, ui)
      {
        $('.ui-dialog-titlebar-close').hide();
      },
      resizable: false,
      draggable: false,
      disabled: true
    });
    window.onbeforeunload = function(e) {
      var confirmationMessage = "The file has not completed uploading.";

      (e || window.event).returnValue = confirmationMessage;     //Gecko + IE
      return confirmationMessage;                                //Webkit, Safari, Chrome etc.
    };
  }

  this.hideProgressDialog = function() {
    $('#progressStage').dialog( "destroy" );
    $('#grayOut').css('display','block');
    window.onbeforeunload = null;
  }

  this.setProgress = function(message) {
    var $progress = $('#progressStage');

    if (message.uploadjob !== undefined) {         // status message or upload progress?
      message = message.uploadjob;
      var filename = message.payload.filename;
      var total = message.payload.totalsize;
      var received;
      var percentage;
      
      $progress.find('.progress-label-top').text('Uploading ' + filename.replace("C:\\fakepath\\", ""));
      
      if(message.payload.totalsize !== -1) {
        received = message['current-chunk']['bytes-recieved'] + message['current-chunk'].number * message.chunksize;
        percentage = ((received / total) * 100).toFixed(1) + '%';
        total = (total / ocUpload.MEGABYTE).toFixed(2) + ' MB';
        received = (received / ocUpload.MEGABYTE).toFixed(2) + ' MB';
        $progress.find('.progressbar-indicator').css('width', percentage);
        $progress.find('.progressbar-label > span').text(percentage);
        $progress.find('.progress-label-left').text(received + ' received');
        $progress.find('.progress-label-right').text(total + ' total');        
      } else {
        received = message['current-chunk']['bytes-recieved'];
        received = (received / ocUpload.MEGABYTE).toFixed(2) + ' MB';
        $progress.find('.progressbar-label > span').text('');
        $progress.find('.progress-label-left').text(received + ' received');
      }
      
      if (message.payload.currentsize == message.payload.totalsize) {
        if(message.state == ocUpload.UPLOAD_COMPLETE) {
          ocUpload.Listener.uploadComplete(message.id, message.payload.url);
        } else {
          ocUpload.UI.setProgress('Processing upload, this may take some time.');
        } 
      }
    } else {
      $progress.find('.upload-label').text(' ');
      $progress.find('.progressbar-indicator').css('width', '0%');
      $progress.find('.progressbar-label > span').text(message);
    }
  }

  this.showSuccess = function() {
    ocUpload.UI.hideProgressDialog();
    ocUpload.UI.showSuccesScreen();
  //ocUpload.backToRecordings();
  //window.location = '/admin';
  }

  this.showFailure = function(message) {
    ocUpload.UI.hideProgressDialog();
    alert("Ingest failed:\n" + message);
    //ocUpload.backToRecordings();
    window.location = '/admin/index.html#/recordings?' + window.location.hash.split('?')[1];;
  }
  
  /**
   * collects metadata to show in sucess screen
   *
   * @return array metadata
   */
  this.collectFormData = function() {
    ocUtils.log("Collecting metadata");

    var metadata = new Array;
    metadata['files'] = new Array();

    $('.oc-ui-form-field').each( function() { //collect text input
      metadata[$(this).attr('name')] = $(this).val();
    });
    $('.uploadForm-container:visible').each(function() { //collect file names
      var file = $(this).contents().find('.file-selector').val();
      if(file != undefined) {
        metadata['files'].push(file);
      }
    });
    this.metadata = metadata;
  }

  /**
   * loads success screen template and fills with data
   */
  this.showSuccesScreen = function() {
    var data = this.metadata;
    $('#stage').load('complete.html', function() {
      for (var key in data) {
        if (data[key] != "" && key != 'files') { //print text, not file names
          $('#field-'+key).css('display','block');
          if (data[key] instanceof Array) {
            $('#field-'+key).children('.fieldValue').text(data[key].join(', '));
          } else {
            $('#field-'+key).children('.fieldValue').text(data[key]);
          }
        }
      }
      $('.field-filename').each(function() { //print file names
        var file = data['files'].shift();
        if(file) {
          $(this).children('.fieldValue').text(file.replace("C:\\fakepath\\", ""));
        } else {
          $(this).hide();
        }
      });
    //When should it show this heading?
    //$('#heading-metadata').text('Your recording with the following information has been resubmitted');
    });
  }



  return this;
})();

/** @namespace Ingest logic
 *
 */
ocUpload.Ingest = (function() {

  var ELEMENT_TYPE = {
    CATALOG : 1,
    TRACK : 2
  };

  var MediaPackage = {
    document : '',
    elements : []
  };

  var Workflow = {
    definition : false,
    properties : {}
  };

  /** Constructor for MediaPackageElement
   */
  function MediaPackageElement(id, type, flavor, payload) {
    this.id = id;
    this.type = type;
    this.flavor = flavor;
    this.payload = payload;
    this.done = false;
  }

  this.begin = function() {
    ocUpload.UI.showProgressDialog();
    ocUpload.UI.setProgress("Constructing Media Package...");

    // enqueue Episode Dublin Core
    MediaPackage.elements.push(
      new MediaPackageElement('episodeDC', ELEMENT_TYPE.CATALOG, 'dublincore/episode', createDublinCoreDocument()));
    ocUtils.log("Added Dublin Core catalog for episode");

    // enqueue Series Dublin Core
    var series = $('#seriesSelect').val();
    //var seriesId = $('#isPartOf').val();
    if (series !== '') {
      var seriesId = $('#isPartOf').val();
      if (seriesId === '') {
        seriesId = createSeries(series);
      }
      MediaPackage.elements.push(
        new MediaPackageElement('seriesDC', ELEMENT_TYPE.CATALOG, 'dublincore/series', getSeriesCatalog(seriesId)));
      ocUtils.log("Added Dublin Core catalog for series");
    }

    // enqueue Tracks
    $('.upload-widget:visible').each(function() {
      var $uploader = $(this).find('.uploadForm-container');
      if ($uploader.contents().find('.file-selector').val() != '') {
        var id = $uploader.contents().find('.track-id').val();
        var flavor = $(this).find('.track-flavor').val();
        MediaPackage.elements.push(new MediaPackageElement(id, ELEMENT_TYPE.TRACK, flavor, $uploader));
        ocUtils.log('Added Track (' + flavor + ')');
      }
    });

    // get workflow configuration
    Workflow.definition = $('#workflowSelector').val();
    Workflow.properties = ocUpload.Ingest.getWorkflowConfiguration($('#workflowConfigContainer'));

    createMediaPackage();   // begin by creating the initial MediaPackage
  }

  function proceed() {
    ocUtils.log('Proceeding with ingest');
    var nextElement = false;

    // search for element to be submitted
    $(MediaPackage.elements).each(function(index, element){
      if (nextElement === false && element.done === false) {
        nextElement = element;
      }
    });

    // submit next MediaPackageElement
    if (nextElement !== false) {
      switch(nextElement.type) {
        case ELEMENT_TYPE.CATALOG:
          addCatalog(nextElement);
          break;
        case ELEMENT_TYPE.TRACK:
          addTrack(nextElement);
          break;
        default:
          break;
      }
    } else {          // all elements added
      ocUtils.log('No more elements to add');
      startIngest();  // start Ingest
    }
  }

  function startIngest() {
    ocUtils.log('Starting Ingest');
    ocUpload.UI.setProgress('Starting Processing...');

    var workflowData = Workflow.properties;
    workflowData['mediaPackage'] = MediaPackage.document;
    $.ajax({
      url : ocUpload.INGEST_START_URL + '/' + Workflow.definition,
      async : true,
      type : 'post',
      data : workflowData,
      error : function() {
        ocUpload.Listener.ingestError('Failed to start Processing');
      },
      success : function() {
        ocUpload.UI.showSuccess();
      }
    });
  }

  this.discardIngest = function() {
    if (MediaPackage.document !== null) {
    // TODO call discardMediaPackage method
    }
  }

  function createMediaPackage() {
    ocUpload.UI.setProgress("Creating Media Package on Server...");
    $.ajax({
      url        : INGEST_CREATE_MP_URL,
      type       : 'GET',
      dataType   : 'xml',                     // TODO try to take the response directly as string
      error      : function(XHR,status,e){
        ocUpload.Listener.ingestError('Could not create MediaPackage');
      },
      success    : function(data, status) {
        MediaPackage.document = ocUtils.xmlToString(data);
        proceed();
      }
    });
  }

  function createDublinCoreDocument() {
    dcDoc = ocUtils.createDoc('dublincore', 'http://www.opencastproject.org/xsd/1.0/dublincore/');
    $(dcDoc.documentElement).attr('xmlns:dcterms', 'http://purl.org/dc/terms/');
    $('.dc-metadata-field').each(function() {
      $field = $(this);
      var $newElm = $(dcDoc.createElement('dcterms:' + $field.attr('name')));
      $newElm.text($field.val());
      $(dcDoc.documentElement).append($newElm);
    });
    var $created = $(dcDoc.createElement('dcterms:created'))
    var date = $('#recordDate').datepicker('getDate').getTime();
    date += $('#startTimeHour').val() * 60 * 60 * 1000;
    date += $('#startTimeMin').val() * 60 * 1000;
    $created.text(ocUtils.toISODate(new Date(date)));
    $(dcDoc.documentElement).append($created);
    var out = ocUtils.xmlToString(dcDoc);
    return out;
  }

  function addCatalog(catalog) {
    ocUtils.log('Uploading Dublin Core Catalog (' + catalog.flavor + ')');
    ocUpload.UI.setProgress("Uploading Catalog (" + catalog.flavor + ")...");
    $.ajax({
      url        : INGEST_ADD_CATALOG_URL,
      type       : 'POST',
      dataType   : 'xml',
      data       : {
        flavor       : catalog.flavor,
        mediaPackage : MediaPackage.document,
        dublinCore   : catalog.payload
      },
      error      : function(XHR,status,e){
        ocUpload.Listener.ingestError('Could not add DublinCore catalog to MediaPackage.');
      },
      success    : function(data) {
        MediaPackage.document = ocUtils.xmlToString(data);
        catalog.done = true;
        proceed();
      }
    });
  }

  function addTrack(track) {
    var $uploader = track.payload;
    var filename = $uploader.contents().find('.file-selector').val();

    ocUtils.log('Uploading ' + filename.replace("C:\\fakepath\\", "") + ' (' + track.id + ')');
    ocUpload.UI.setProgress("Uploading " + filename.replace("C:\\fakepath\\", ""));

    var uploaderType = $uploader.contents().find('#uploader-type').val();

    // set flavor and mediapackage in upload form before submit
    $uploader.contents().find('#flavor').val(track.flavor);
    $uploader.contents().find('#mediapackage').val(MediaPackage.document);
    
    if(uploaderType == 'upload') {
    	track.id = createUploadJob($uploader);
    	track.flavor = $uploader.parent().find('input.track-flavor').val();
    	
    	//upload mediapackage to upload service
    	
    	if(ocUtils.isChunkedUploadCompliable()) {
    		ocUtils.log("Uploading via Chunked upload")
    		var file = $uploader.contents().find('.file-selector')[0].files[0];
    		nextPart(file, 0, track.id, 0, ocUpload.CHUNKSIZE);
    	} else {
    		ocUtils.log("Uploading via submitting form")
    		$uploader.contents().find('#uploadForm').submit();
    	}
    	
    	ocUpload.Listener.startProgressUpdate(track.id);
    }
    else if(uploaderType == 'inbox') {
    	$uploader.contents().find('#uploadForm').submit();
    }
  }
  
  function createUploadJob($uploader) {
    var filesize = -1;
    var chunksize = -1;
    var filename = $uploader.contents().find('.file-selector').val();
    filename = filename.replace("C:\\fakepath\\", "");
    
    var track_id = "";
    
    if (ocUtils.isChunkedUploadCompliable()) {
      var files = $uploader.contents().find('.file-selector')[0].files;
      var file = files[0];
      filesize = file.size;
      chunksize = CHUNKSIZE;
    }
    $.ajax({
      type: 'POST',
      url: ocUpload.CREATE_NEW_JOB_URL,
      async: false,
      data: {
        filename : filename,
        filesize : filesize,
        chunksize: chunksize,
        flavor   : $uploader.parent().find('input.track-flavor').val(),
        mediapackage : MediaPackage.document
      },
      success: function(job_id) {
        track_id = job_id;
        $uploader.contents().find('.track-id').val(job_id);
        $uploader.contents().find('#uploadForm').attr('action', UPLOAD_URL + job_id);
      }
    });
    return track_id;
  }
  
  function nextPart(file, chunk, jobId, start, end) {
    if(start < file.size) {
      ocUtils.log("uploading chunk #" + chunk);
      var blob;
      if ('mozSlice' in file) { //Mozilla
        blob = file.mozSlice(start, end);
      } else if('webkitSlice' in file){ //Webkit
        blob = file.webkitSlice(start, end);
      } else { //Opera
        blob = file.slice(start, end);
      }
      upload(blob, file, chunk, jobId, start, end);
    }
  }
  
  function upload(blob, file, chunk, jobId, start, end) {
    var formData = new FormData();
    formData.append("chunknumber", chunk);
    formData.append("jobID", jobId);
    formData.append("filedata", blob);
    
    $.ajax({
      url: UPLOAD_CHUNK_URL + jobId,
      data: formData,
      processData: false,
      type: 'POST',
      cache: false,
      contentType: false,
      success: function (e, status, jqHBX) {
        if(jqHBX.status >= 200 && jqHBX.status < 300)  {
          nextPart(file, ++chunk, jobId, start + ocUpload.CHUNKSIZE, end + ocUpload.CHUNKSIZE);
        }
      },
      error: function receiveError(jqXHR, textStatus, errorThrown) {
        ocUpload.UI.hideProgressDialog();
        alert("Upload failed:\n" + errorThrown);
        window.location = '/admin/index.html#/recordings?' + window.location.hash.split('?')[1];;
      }
    });
  }
  
  this.trackDone = function(jobId, trackUrl) {
    var track = null;
    $(MediaPackage.elements).each(function(index, element) {
      if (element.id == jobId) {
        track = element;
      }
    });
    MediaPackage.document = ocMediapackage.addTrack(MediaPackage.document, trackUrl, track.flavor);
    
    track.done = true;
    proceed();
  }

  function createSeries(name) {
    var id = false;
    var seriesXml = '<dublincore xmlns="http://www.opencastproject.org/xsd/1.0/dublincore/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:oc="http://www.opencastproject.org/matterhorn/"><dcterms:title xmlns="">' + ocUtils.escapeXML(name) + '</dcterms:title></dublincore>';
    var anonymous_role = 'anonymous';

    ocUpload.UI.setProgress("Creating Series " + name);
    $.ajax({
      url: ocUpload.ANOYMOUS_URL,
      type: 'GET',
      dataType: 'json',
      async: false,
      error: function () {
        if (ocUtils !== undefined) {
          ocUtils.log("Could not retrieve anonymous role " + ocUpload.ANOYMOUS_URL);
        }
      },
      success: function(data) {
        anonymous_role = data.org.anonymousRole;
      }
    });
    $.ajax({
      async: false,
      type: 'POST',
      url: ocUpload.SERIES_URL,
      data: {
        series: seriesXml,
        acl: '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><acl xmlns="http://org.opencastproject.security"><ace><role>' + anonymous_role + '</role><action>read</action><allow>true</allow></ace></acl>'
      },
      dataType : 'xml',
      error: function() {
        ocUpload.Listener.ingestError('Could not create Series ' + name);
      },
      success: function(data){
        window.debug = data;
        id = $(data).find('[nodeName="dcterms:identifier"]').text();
      }
    });
    return id;
  }

  function getSeriesCatalog(id) {
    var catalog = null;
    ocUpload.UI.setProgress("Loading Series Catalog");
    $.ajax({
      url : '/series/' + id + '.xml',
      type : 'get',
      async : false,
      dataType : 'xml',              // TODO try to take the response directly as string
      error : function() {
        ocUpload.Listener.ingestError("Could not get Series Catalog");
      },
      success : function(data) {
        catalog = ocUtils.xmlToString(data);
      }
    });
    return catalog;
  }

  this.getWorkflowConfiguration = function($container) {
    var out = new Object();
    $container.find('.configField').each( function(idx, elm) {
      if ($(elm).is('[type=checkbox]')) {
        if ($(elm).is(':checked')) {
          out[$(elm).attr('id')] = $(elm).val();
	    console.log(out);
        }
      } else {
        out[$(elm).attr('id')] = $(elm).val();
      }
    });
    return out;
  }

  return this;
})();

/** @namespace Listener for Upload Events
 *
 */
ocUpload.Listener = (function() {

  var Update = {
    id : false,
    jobId : null,
    inProgress : false
  }


  this.uploadComplete = function(jobId, trackUrl) {
    destroyUpdateInterval();
    ocUtils.log("Upload complete " + jobId);
    ocUpload.UI.setProgress('Upload successful');
    ocUpload.Ingest.trackDone(jobId, trackUrl);
    window.onbeforeunload = null;
  }

  this.uploadFailed = function(jobId) {
    ocUtils.log("ERROR: Upload failed " + jobId);
    destroyUpdateInterval();
    ocUpload.Listener.ingestError('Upload has failed!');
  }

  this.ingestError = function(message) {
    ocUpload.Ingest.discardIngest();
    ocUpload.UI.showFailure(message);
  }

  this.startProgressUpdate = function(jobId) {
    Update.inProgress = false;
    Update.jobId = jobId;
    Update.id = window.setInterval(requestUpdate, ocUpload.UPLOAD_PROGRESS_INTERVAL);
  };

  function requestUpdate() {
    if (!Update.inProgress && Update.id !== null) {
      Update.inProgress = true;
      $.ajax({
        url : ocUpload.INGEST_PROGRESS_URL + '/' + Update.jobId + ".json",
        type : 'get',
        dataType : 'json',
        success : receiveUpdate,
        error: receiveError
      });
    }
  }

  function receiveError(jqXHR, textStatus, errorThrown) {
    var httpStatus = textStatus + ', ' + errorThrown;
    var logEntry = Update.jobId + ' [' + httpStatus + ']';
    ocUpload.UI.showFailure("Upload failed with: " + httpStatus);
    uploadFailed(logEntry);
  }

  function receiveUpdate(data) {
    Update.inProgress = false;
    ocUpload.UI.setProgress(data);
  }

  function destroyUpdateInterval() {
    window.clearInterval(Update.id);
    Update.id = null;
  }

  return this;
})();
