<!-- <div id="stage" class="ui-widget">
  <div id="controlsTopSecurity" class="ui-helper-clearfix">
    <div class="state-filter-container">
      <input type="radio" name="stateSelect" value="groups" id="security-groups" /><label for="security-groups">Groups</label>
      <input type="radio" name="stateSelect" value="acl" id="security-acl" /><label for="security-acl">ACL</label>
    </div>
  </div>
  <div id="addGroup" class="ui-widget ui-helper-clearfix">
    <h3>Add Group<h3>
    <img class="ui-icon ui-icon-circle-plus"></img>
  </div>
  <div id="tableContainer" class="ui-widget ui-helper-clearfix"></div>
  <div id="groups-form" style="display:none;" title="Create new group"></div>
  <div id="addAcl" class="ui-widget ui-helper-clearfix">
    <h3>Add ACL<h3>
    <img class="ui-icon ui-icon-circle-plus"></img>
  </div>
  <div id="aclContainer" class="ui-widget ui-helper-clearfix"></div>
  <div id="acl-form" style="display:none;" title="Create new ACL"></div>
</div> -->
<div class="form-box layout-centered ui-widget">
      <div class="form-box-head ui-widget-header ui-corner-top oc-ui-cursor">
        <div id="i18n_new_account">Create Account</div>
        <div class="clear"></div>
      </div>
      <div class="form-box-content ui-widget-content ui-corner-bottom">
        <form action="">
          <ul class="oc-ui-form-list">
          	<li>
              <label for="firstName" id="firstNameLabel"><span>First Name</span><span class="scheduler-required-text"> *</span>:</label>
              <input type="text" class="oc-ui-form-field" name="firstName" id="firstName" maxlength="255" />
            </li>
            <li>
              <label for="lastName" id="lastNameLabel"><span>Last Name</span><span class="scheduler-required-text"> *</span>:</label>
              <input type="text" class="oc-ui-form-field" name="lastName" id="newAccount" maxlength="255" />
            </li>
            <li>
              <label for="email" id="emailLabel"><span>Email</span><span class="scheduler-required-text"> *</span>:</label>
              <input type="text" class="oc-ui-form-field" name="email" id="email" maxlength="255" />
            </li>
            <li>
            	<label for="mobile" id="mobileLabel"><span>Mobile Number</span><span class="schedular-required-text"> *</span>:</label>
            	<input type="text" class="oc-ui-form-field" name="mobile" id="mobile" maxlength="255" />
            </li>
            <li>
				<button id="createAccount" type="button">Create Account</button><button id="cancelAccount" type="button">Cancel</button>
			</li>
          </ul>
        </form>
      </div>
 </div>