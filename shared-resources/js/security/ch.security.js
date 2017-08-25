/**
* Manage (Create, Update, Delete) User in capehub user directory
*/
chSecurity = new (function() {
	
var self = this;
	
	var BASE_URL = "/capehub";
	
	/** 
	 * Create new user in the Capehub user directory
	 */
	self.createUser = function(user, roles) {
		$.ajax({
			type: 'POST',
			url: '/users',
			data: {
				user: user,
				roles: roles,
			},
			success: function() {
				//refresh the page
			}
		})
	}
	
	/**
	 * Update existing user in the Capehub user directory
	 */
	self.updateUser = function(user, roles, id) {
		$.ajax({
			type: 'PUT',
			url: '/users/' + id,
			data: {
				user: user,
				roles: roles
			},
			success: function() {
				//refresh the page
			}
		})
	}
	
	this.createGroup = function(name, roles, users) {
	      $.ajax({
	    	  type: 'POST',
	    	  url: "/groups",
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
	  
	  this.updateGroup = function(id, roles, users) {
		  $.ajax({
			  type: 'PUT',
			  url: "/groups/" + id,
			  data: {
				  roles: roles,
				  users: users
			  },
			  success: function() {
				  //refresh the page
			  }
		  });
	  }
  
});