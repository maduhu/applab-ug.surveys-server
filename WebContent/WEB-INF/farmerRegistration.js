function toggle(elementId) {
	var element = document.getElementById(elementId);
	if (element.style.display != 'none') {
		element.style.display = 'none';
	} else {
		element.style.display = '';
	}
}

function toggleExpando(elementId) {
	var element = document.getElementById(elementId);
	if (element.innerHTML == '(Hide)') {
		element.innerHTML = '(Show)';
	} else {
		element.innerHTML = '(Hide)';
	}
}

function validateRequired(field, alertText) {
	with (field) {
		if (value == null || value == "") {
			alert(alertText);
			field.focus();
			return false;
		} else {
			return true;
		}
	}
}

function validateRadioButtons(field, alertText) {
	option = -1;
	for (i = field.length - 1; i > -1; i--) {
		if (field[i].checked) {
			option = i;
			i = -1;
		}
	}
	if (option == -1) {
		alert(alertText);
		return false;
	} else {
		return true;
	}
}

function validate(form) {
	with (form) {
		if (!validateRequired(farmerId, "Farmer ID must not be blank")) {
			return false;
		}
		if (!validateRequired(firstName, "First Name must not be blank")) {
			return false;
		}
		if (!validateRequired(lastName, "Last Name must not be blank")) {
			return false;
		}
		if (!validateRequired(fatherName, "Father's Name must not be blank")) {
			return false;
		}
		if (!validateRadioButtons(gender, "Gender is a required field")) {
			return false;
		}
		if (!validateRequired(village, "Village must not be blank")) {
			return false;
		}
		if (!validateRadioButtons(householdStatus,
				"Household Status is a required field")) {
			return false;
		}

	}
}
