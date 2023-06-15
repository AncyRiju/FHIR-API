/*
* AMRIT – Accessible Medical Records via Integrated Technology 
* Integrated EHR (Electronic Health Records) Solution 
*
* Copyright (C) "Piramal Swasthya Management and Research Institute" 
*
* This file is part of AMRIT.
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see https://www.gnu.org/licenses/.
*/
package com.wipro.fhir.r4.service.resource_model;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wipro.fhir.r4.data.patient.PatientDemographic;
import com.wipro.fhir.r4.data.request_handler.ResourceRequestHandler;
import com.wipro.fhir.r4.repo.common.PatientEligibleForResourceCreationRepo;
import com.wipro.fhir.r4.repo.healthID.BenHealthIDMappingRepo;
import com.wipro.fhir.r4.service.api_channel.APIChannel;
import com.wipro.fhir.r4.service.common.CommonServiceImpl;
import com.wipro.fhir.r4.utils.exception.FHIRException;

/***
 * 
 * @author NE298657
 *
 */

@Service
public class PatientResource {

	@Autowired
	private APIChannel aPIChannel;
	@Autowired
	private CommonServiceImpl commonServiceImpl;

	private Patient p;
	@Autowired
	private BenHealthIDMappingRepo benHealthIDMappingRepo;

	@Autowired
	private PatientEligibleForResourceCreationRepo patientEligibleForResourceCreationRepo;

	@Autowired
	private PatientDemographic patientDemographic;

	public Patient getPatientResource(ResourceRequestHandler resourceRequestHandler) throws FHIRException {

		// call BenID search API through API channel from platform
//		String responseBody = aPIChannel.benSearchByBenID(Authorization, resourceRequestHandler);
//
//		PatientSearchAPIResponse psr = (InputMapper.gson().fromJson(responseBody, PatientSearchAPIResponse.class));

		List<Object[]> rsObjList = patientEligibleForResourceCreationRepo
				.callPatientDemographicSP(resourceRequestHandler.getBeneficiaryRegID());

		PatientDemographic patientDemographicOBJ = patientDemographic.getPatientDemographic(rsObjList);

		if (patientDemographicOBJ != null) {
			if (patientDemographicOBJ.getBeneficiaryRegID() != null) {
				return generatePatientResource(patientDemographicOBJ);
			} else
				throw new FHIRException("multiple patient found with given identifier / beneficiaryID");
		} else
			throw new FHIRException("patient not found");
	}

	private Patient generatePatientResource(PatientDemographic pd) {
		String UUID = commonServiceImpl.getUUID();
		p = new Patient();

		p.setId("Patient/" + pd.getBeneficiaryID());

		// get ABHA for patient - later will be available in patient search only.
		// temp code - till search API is integrated with ABHA
//		ArrayList<BenHealthIDMapping> healthIDMappedList = benHealthIDMappingRepo
//				.getHealthDetails(pd.getBeneficiaryRegID());

		// list of identifiers
		List<Identifier> identifierList = new ArrayList<Identifier>();
		Identifier identifiers;

		if (pd.getHealthIdNo() != null) {

			// ABHA no
			identifiers = new Identifier();
			CodeableConcept cc = new CodeableConcept();
			Coding c = new Coding();
			c.setSystem("http://terminology.hl7.org/CodeSystem/v2-0203");
			c.setCode("MR");
			c.setDisplay(pd.getHealthIdNo());
			cc.addCoding(c);
			identifiers.setType(cc);
			identifierList.add(identifiers);
		}

		p.setIdentifier(identifierList);

		// name
		HumanName hName = new HumanName();
		// title, prefix
//		String title = "";
//		if (pd.getM_title() != null && pd.getM_title().getTitleName() != null)
//			title = pd.getM_title().getTitleName() + " ";

		// hName.addPrefix(title);
		// hName.addGiven(pd.getFirstName());
		// hName.setFamily((pd.getLastName() != null) ? pd.getLastName() : "");
		if (pd.getName() != null)
			hName.setText(pd.getName());
		p.addName(hName);

		// telecom /phone no
//		if (pd.getBenPhoneMaps() != null && pd.getBenPhoneMaps().size() > 0) {
//
//			List<ContactPoint> cpList = new ArrayList<>();
//			ContactPoint cp = new ContactPoint();
//			cp.setSystem(ContactPointSystem.PHONE);
//			cp.setValue(pd.getBenPhoneMaps().get(0).getPhoneNo());
//			cp.setUse(ContactPointUse.MOBILE);
//		}

		// gender
		if (pd.getGender() != null) {
			switch (pd.getGender()) {
			case "Male":
				p.setGender(AdministrativeGender.MALE);
				break;
			case "Female":
				p.setGender(AdministrativeGender.FEMALE);
				break;
			case "Transgender":
				p.setGender(AdministrativeGender.OTHER);
				break;
			default:
				p.setGender(AdministrativeGender.UNKNOWN);
				break;
			}
		} else {
			if (pd.getGenderID() != null) {
				switch (pd.getGenderID()) {
				case 1:
					p.setGender(AdministrativeGender.MALE);
					break;
				case 2:
					p.setGender(AdministrativeGender.FEMALE);
					break;
				case 3:
					p.setGender(AdministrativeGender.OTHER);
					break;

				default:
					p.setGender(AdministrativeGender.UNKNOWN);
					break;
				}
			}
		}

		// DOB
		if (pd.getDOB() != null) {
			p.setBirthDate(pd.getDOB());
		}

		// Address
//		if (pd.getI_bendemographics().getAddressLine1() != null || pd.getI_bendemographics().getAddressLine1() != null
//				|| pd.getI_bendemographics().getAddressLine1() != null) {
//			String address1 = (pd.getI_bendemographics().getAddressLine1() != null)
//					? pd.getI_bendemographics().getAddressLine1()
//					: "";
//			String address2 = (pd.getI_bendemographics().getAddressLine2() != null)
//					? pd.getI_bendemographics().getAddressLine2()
//					: "";
//			String address3 = (pd.getI_bendemographics().getAddressLine3() != null)
//					? pd.getI_bendemographics().getAddressLine3()
//					: "";
//
//			p.addAddress().setText(address1 + " " + address2 + " " + address3);
//		}
//		if (pd.getI_bendemographics().getPinCode() != null)
//			p.addAddress().setPostalCode(pd.getI_bendemographics().getPinCode());
//		if (pd.getI_bendemographics().getDistrictName() != null)
//			p.addAddress().setDistrict(pd.getI_bendemographics().getDistrictName());
//		if (pd.getI_bendemographics().getStateName() != null)
//			p.addAddress().setState(pd.getI_bendemographics().getStateName());
//		p.addAddress().setCountry("India");

		// Martial status
//		if (pd.getMaritalStatus().getMaritalStatusID() != null && pd.getMaritalStatus().getStatus() != null) {
//			CodeableConcept cc = new CodeableConcept();
//			Coding c = new Coding();
//
//			c.setCode(pd.getMaritalStatus().getMaritalStatusID().toString());
//			c.setDisplay(pd.getMaritalStatus().getStatus());
//
//			cc.addCoding(c);
//
//			p.setMaritalStatus(cc);
//		}

		return p;
	}
}
