
var chUsers = chUsers || {};
chUsers.users = [];

chUsers.init = function () {
  $.ajax( {
    url: '/users/users.json?limit=1000',
    type: 'GET',
    success: chUsers.buildUsersView
  })
        
}

chUsers.buildUsersView = function(data) {
	chUsers.users = data.users.user;
  $.each(chUsers.users, function (key, user) {
	var roles = [];
	$.each(user.roles.role, function (key, role) {
	  roles.push(role.name);
	});
    user.roles = roles.sort().join(', ');
  });
  $('#addHeader').jqotesubtpl('templates/users.tpl', {users: chUsers.users});
  // Attach actions to the update buttons
  $(".roleButton").each(function (i) {
    $(this).click(function() {
      // POST the role array to /users/[username].json
      var row = $(this).parent().parent();
      var username = this.id.substring(7); // "button_" = 7 characters
      var url = "/users/" + username + ".json";
      var roleArray = $("#text_" + username).val().split(",");
      var roles = "[";
      for(i =0; i < roleArray.length; i++) {
        roles +="\"";
        roles += $.trim(roleArray[i]);
        roles +="\"";
        if(i < roleArray.length -1) {
          roles += ",";
        }
      }
      roles +="]";
      $.ajax({
        url: url,
        type: 'PUT',
        dataType: 'text',
        data: {
          "roles": roles
        },
        success: function() {
          row.fadeOut('slow', function() {
            row.fadeIn();
          });
        }
      });
    });
  });
}