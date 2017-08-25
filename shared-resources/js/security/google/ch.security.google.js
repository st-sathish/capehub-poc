/**
* Manage (Create, Update, Delete) User in Google user directory
*/

chSecurityGoogle = new (function() {
	
	var self = this;
	
	// this var holds the actual refresh function and gets initialized lazily
	 var _theRefreshFunc = function () {return $.Deferred()};
	 
	 var refreshing = false; // indicates if JSONP requesting is in progress
	 
	  /** 
	   * Executed when directly when script is loaded: parses url parameters and
	   * returns the configuration object.
	   */
	  this.Configuration = new (function() {
	
	    // default configuartion
	    this.state = 'user';
	    this.refresh = 10000;
	
	    // parse url parameters
	    try {
	      var p = document.location.href.split('?', 2)[1] || false;
	      if (p !== false) {
	        p = p.split('&');
	        for (i in p) {
	          var param = p[i].split('=');
	          if (this[param[0]] !== undefined) {
	            this[param[0]] = unescape(param[1]);
	          }
	        }
	      }
	    } catch (e) {
	      alert('Unable to parse url parameters:\n' + e.toString());
	    }
	
	    return this;
	  })();
	  
	/** 
	 * Create new user in the google user directory
	 */
	self.createUser = function(user, roles) {
		$.ajax({
			type: 'POST',
			url: '/google-user-manager/users',
			data: {
				user: user,
				roles: roles,
			},
			success: function() {

			}
		})
	}
	
	/**
	 * Update existing user in the google user directory
	 */
	self.updateUser = function(user, roles, id) {
		$.ajax({
			type: 'PUT',
			url: '/google-user-manager/users/' + id,
			data: {
				user: user,
				roles: roles
			},
			success: function() {
				//refresh the page
			}
		})
	}
	
	/**
	 * Create new group
	 */
	this.createGroup = function(name, roles, users) {
	      $.ajax({
	    	  type: 'POST',
	    	  url: "/google-user-manager/groups",
	    	  data: {
	    		  name: name,
	    		  roles: roles,
	    		  users: users
	    	  },
	    	  success: function() {
	    		//refresh the page
	    	  }
	      });
	  }
	  
	/**
	 * Update the group
	 */
	  this.updateGroup = function(id, roles, users) {
		  $.ajax({
			  type: 'PUT',
			  url: "/google-user-manager/groups/" + id,
			  data: {
				  roles: roles,
				  users: users
			  },
			  success: function() {
				  //refresh the page
			  }
		  });
	  }

	  this.refresh = function() {
		  return _theRefreshFunc();
	  }
	  
	  /** 
	   *  Initiate new ajax call to groups list endpoint
	   *  @return deferred object
	   */
	  this._refresh = function() {
	    if (!refreshing) {
	      //refreshing = true;
	      // issue the ajax request
	      return $.Deferred(function (d) {
	    	if(chSecurityGoogle.Configuration.state == 'groups') {
	    		$('#groups-form').show();
	  	      	$('#user-form').hide();
	  	      	$('#acl-form').hide();
	    	} else if(chSecurityGoogle.Configuration.state == 'acl') {
	    		$('#groups-form').hide();
	  	      	$('#user-form').hide();
	  	      	$('#acl-form').show();
	    	} else {
	    		$('#groups-form').hide();
	  	      	$('#user-form').show();
	  	      	$('#acl-form').hide();
	    	}
	      });
	    } else {
	      // return an empty deferred
	      return $.Deferred();
	    }
	  }
	  
	  this.getOptions = function(select) {
		  var values = [];
		  select.find('option').each(function(i, option){
			values.push($(option).val());
		  });
		  return values.join();
	  }
	  
	  this.initUserForm = function() {
		  $('#user-form').jqotesubtpl('templates/security/security-user.tpl', {});
		  var firstName = $("#user-form #firstName");
		  var lastName = $("#user-form #lastName");
		  var email = $("#user-form #email");
		  var mobileNumber = $("#user-form #mobileNumber");
		  var createAccBtn = $("#user-form button#createAccount");
		  var cancelBtn = $("#user-form button#cancel");
		  createAccBtn.button().click(function(event) {
			  var u = {
				'firstName':firstName.val(),
				'lastName': lastName.val(),
				'email': email.val(),
				'mobileNumber': mobileNumber.val()
			  };
			  var roles = [];
			  self.createUser(u, roles);
		  });
		  cancelBtn.button().click(function(event) {
			  
		  });
	  }
	  
	  this.initGroupForm = function() {
		  $('#groups-form').jqotesubtpl('templates/security/security-groups.tpl', {});
		  
	  }
	  
	  this.initAclForm = function() {
		  $('#acl-form').jqotesubtpl('templates/security-acl.tpl', {});
		  
	  }
	  
	  /** 
	   * $(document).ready()
	   */
	  this.init = function() {
		  $('#addHeader').jqotesubtpl('templates/security-header.tpl', {});
		  
		  // chSecurity state selectors
		  $('#security-' +  chSecurityGoogle.Configuration.state).attr('checked', true);
		  $('.state-filter-container').buttonset();
		  
		  //enable later
		  $('.state-filter-container input').click(function() {
			  chSecurityGoogle.Configuration.state = $(this).val();
			  self.refresh();
		  });
		  
		  self.initUserForm();
		  self.initGroupForm();
		  self.initAclForm();
		  
		  _theRefreshFunc = function () {
		      return self._refresh();
		   };
		   
		   self.refresh();
		   
	      //set workflow
	      //$('#processingNotification').jqotesubtpl('templates/processing-instructions.tpl', {});
	      //chWorkflow.init($('#workflowSelector'), $('#workflowConfigContainer'), ['email']);
	  };
	  
	  return this;
})();