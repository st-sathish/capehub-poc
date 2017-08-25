<p class="validateTips">Field's Marked as <span class="required-text">*</span> are required.</p>
<form>
	<div class="form-box layout-centered ui-widget">
      <div class="form-box-head ui-widget-header ui-corner-top oc-ui-cursor">
        <div id="i18n_new_account">Create Account</div>
        <div class="clear"></div>
      </div>
      <div class="form-box-content ui-widget-content ui-corner-bottom">
        <ul class="oc-ui-form-list">
          	<li>
              <label for="firstName" id="firstNameLabel"><span>First Name</span><span class="required-text"> *</span>:</label>
              <input type="text" class="oc-ui-form-field" name="firstName" id="firstName" maxlength="255" />
            </li>
            <li>
              <label for="lastName" id="lastNameLabel"><span>Last Name</span><span class="required-text"> *</span>:</label>
              <input type="text" class="oc-ui-form-field" name="lastName" id="lastName" maxlength="255" />
            </li>
            <li>
              <label for="email" id="emailLabel"><span>Email</span><span class="required-text"> *</span>:</label>
              <input type="text" class="oc-ui-form-field" name="email" id="email" maxlength="255" />
            </li>
            <li>
            	<label for="mobileNumber" id="mobileNumberLabel"><span>Mobile Number</span><span class="required-text"> *</span>:</label>
            	<input type="text" class="oc-ui-form-field" name="mobileNumber" id="mobileNumber" maxlength="255" />
            </li>
          </ul>
      </div>
	</div>
	
	<!-- Processing Instructions -->
	<div id="processingNotification"></div>
              
	<div>
		 <button id="createAccount" type="button">Create Account</button><button id="cancelAccount" type="button">Cancel</button>
	</div>
</form>