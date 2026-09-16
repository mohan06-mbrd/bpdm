/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.orchestrator.mapper.v6

import org.eclipse.tractusx.bpdm.orchestrator.mapper.BusinessPartnerRequestMapper
import org.eclipse.tractusx.bpdm.orchestrator.model.request.BusinessPartnerRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.LegalEntityRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.PostalAddressWithScriptVariantsRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.GoldenRecordTaskCreateRequest
import org.eclipse.tractusx.orchestrator.api.v6.model.AlternativeAddressV6
import org.eclipse.tractusx.orchestrator.api.v6.model.BpnReferenceTypeV6
import org.eclipse.tractusx.orchestrator.api.v6.model.BusinessPartnerV6
import org.eclipse.tractusx.orchestrator.api.v6.model.BusinessStateV6
import org.eclipse.tractusx.orchestrator.api.v6.model.ConfidenceCriteriaV6
import org.eclipse.tractusx.orchestrator.api.v6.model.GeoCoordinateV6
import org.eclipse.tractusx.orchestrator.api.v6.model.IdentifierV6
import org.eclipse.tractusx.orchestrator.api.v6.model.LegalEntityV6
import org.eclipse.tractusx.orchestrator.api.v6.model.NamePartTypeV6
import org.eclipse.tractusx.orchestrator.api.v6.model.NamePartV6
import org.eclipse.tractusx.orchestrator.api.v6.model.PhysicalAddressV6
import org.eclipse.tractusx.orchestrator.api.v6.model.PostalAddressV6
import org.eclipse.tractusx.orchestrator.api.v6.model.SiteV6
import org.eclipse.tractusx.orchestrator.api.v6.model.StreetV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskCreateRequestEntryV6
import org.eclipse.tractusx.orchestrator.api.v6.model.UncategorizedPropertiesV6
import org.springframework.stereotype.Component

/**
 * Translates the fully isolated V6 create-task request entry into the unified [GoldenRecordTaskCreateRequest].
 * V6 now owns its entire DTO graph, so this mapper explicitly converts every V6 type to the internal request model
 * while defaulting the V7-only/internal fields that do not exist on the frozen V6 contract.
 */
@Component
class GoldenRecordTaskCreateInboundMapperV6(
    private val businessPartnerRequestMapper: BusinessPartnerRequestMapper
) {

    fun toRequest(entry: TaskCreateRequestEntryV6): GoldenRecordTaskCreateRequest =
        GoldenRecordTaskCreateRequest(
            recordId = entry.recordId,
            businessPartner = toBusinessPartnerRequest(entry.businessPartner)
        )

    private fun toBusinessPartnerRequest(businessPartner: BusinessPartnerV6): BusinessPartnerRequest =
        with(businessPartner) {
            BusinessPartnerRequest(
                nameParts = nameParts.map(::toNamePartRequest),
                owningCompany = owningCompany,
                uncategorized = toUncategorizedPropertiesRequest(uncategorized),
                legalEntity = toLegalEntityRequest(legalEntity),
                site = site?.let(::toSiteRequest),
                additionalAddress = additionalAddress?.let {
                    PostalAddressWithScriptVariantsRequest(
                        postalProperties = toPostalAddressRequest(it),
                        scriptVariants = emptyList()
                    )
                },
                additionalSites = emptyList()
            )
        }

    private fun toLegalEntityRequest(legalEntity: LegalEntityV6): LegalEntityRequest =
        with(legalEntity) {
            LegalEntityRequest(
                bpnReference = toBpnReferenceRequest(bpnReference),
                legalName = legalName,
                legalShortName = legalShortName,
                legalForm = legalForm,
                identifiers = identifiers.map(::toIdentifierRequest),
                states = states.map(::toBusinessStateRequest),
                confidenceCriteria = toConfidenceCriteriaRequest(confidenceCriteria),
                isParticipantData = isCatenaXMemberData,
                hasChanged = hasChanged,
                ownershipUltimate = null,
                ultimateOwnerBpnl = null,
                legalAddress = toPostalAddressRequest(legalAddress),
                scriptVariants = emptyList(),
                goldenRecordRelations = emptyList(),
                updatedAt = null
            )
        }

    private fun toNamePartRequest(namePart: NamePartV6) =
        businessPartnerRequestMapper.toNamePartRequest(
            org.eclipse.tractusx.orchestrator.api.model.NamePart(
                name = namePart.name,
                type = when (namePart.type) {
                    NamePartTypeV6.LegalName -> org.eclipse.tractusx.orchestrator.api.model.NamePartType.LegalName
                    NamePartTypeV6.ShortName -> org.eclipse.tractusx.orchestrator.api.model.NamePartType.ShortName
                    NamePartTypeV6.LegalForm -> org.eclipse.tractusx.orchestrator.api.model.NamePartType.LegalForm
                    NamePartTypeV6.SiteName -> org.eclipse.tractusx.orchestrator.api.model.NamePartType.SiteName
                    NamePartTypeV6.AddressName -> org.eclipse.tractusx.orchestrator.api.model.NamePartType.AddressName
                }
            )
        )

    private fun toUncategorizedPropertiesRequest(uncategorized: UncategorizedPropertiesV6) =
        org.eclipse.tractusx.bpdm.orchestrator.model.request.UncategorizedPropertiesRequest(
            nameParts = uncategorized.nameParts,
            identifiers = uncategorized.identifiers.map(::toIdentifierRequest),
            states = uncategorized.states.map(::toBusinessStateRequest),
            address = uncategorized.address?.let {
                PostalAddressWithScriptVariantsRequest(
                    postalProperties = toPostalAddressRequest(it),
                    scriptVariants = emptyList()
                )
            }
        )

    private fun toSiteRequest(site: SiteV6) =
        org.eclipse.tractusx.bpdm.orchestrator.model.request.SiteRequest(
            bpnReference = toBpnReferenceRequest(site.bpnReference),
            siteName = site.siteName,
            states = site.states.map(::toBusinessStateRequest),
            confidenceCriteria = toConfidenceCriteriaRequest(site.confidenceCriteria),
            hasChanged = site.hasChanged,
            siteMainAddress = site.siteMainAddress?.let(::toPostalAddressRequest),
            scriptVariants = emptyList(),
            goldenRecordRelations = emptyList(),
            updatedAt = null
        )

    private fun toIdentifierRequest(identifier: IdentifierV6) =
        businessPartnerRequestMapper.toIdentifierRequest(
            org.eclipse.tractusx.orchestrator.api.model.Identifier(identifier.value, identifier.type, identifier.issuingBody)
        )

    private fun toBusinessStateRequest(state: BusinessStateV6) =
        businessPartnerRequestMapper.toBusinessStateRequest(
            org.eclipse.tractusx.orchestrator.api.model.BusinessState(state.validFrom, state.validTo, state.type)
        )

    private fun toConfidenceCriteriaRequest(confidenceCriteria: ConfidenceCriteriaV6) =
        businessPartnerRequestMapper.toConfidenceCriteriaRequest(
            org.eclipse.tractusx.orchestrator.api.model.ConfidenceCriteria(
                confidenceCriteria.sharedByOwner,
                confidenceCriteria.checkedByExternalDataSource,
                confidenceCriteria.numberOfSharingMembers,
                confidenceCriteria.lastConfidenceCheckAt,
                confidenceCriteria.nextConfidenceCheckAt,
                confidenceCriteria.confidenceLevel
            )
        )

    private fun toBpnReferenceRequest(bpnReference: org.eclipse.tractusx.orchestrator.api.v6.model.BpnReferenceV6) =
        businessPartnerRequestMapper.toBpnReferenceRequest(
            org.eclipse.tractusx.orchestrator.api.model.BpnReference(
                referenceValue = bpnReference.referenceValue,
                desiredBpn = bpnReference.desiredBpn,
                referenceType = bpnReference.referenceType?.let {
                    when (it) {
                        BpnReferenceTypeV6.Bpn -> org.eclipse.tractusx.orchestrator.api.model.BpnReferenceType.Bpn
                        BpnReferenceTypeV6.BpnRequestIdentifier -> org.eclipse.tractusx.orchestrator.api.model.BpnReferenceType.BpnRequestIdentifier
                    }
                }
            )
        )

    private fun toPostalAddressRequest(postalAddress: PostalAddressV6) =
        businessPartnerRequestMapper.toPostalAddressRequest(
            org.eclipse.tractusx.orchestrator.api.model.PostalAddress(
                bpnReference = org.eclipse.tractusx.orchestrator.api.model.BpnReference(
                    postalAddress.bpnReference.referenceValue,
                    postalAddress.bpnReference.desiredBpn,
                    postalAddress.bpnReference.referenceType?.let {
                        when (it) {
                            BpnReferenceTypeV6.Bpn -> org.eclipse.tractusx.orchestrator.api.model.BpnReferenceType.Bpn
                            BpnReferenceTypeV6.BpnRequestIdentifier -> org.eclipse.tractusx.orchestrator.api.model.BpnReferenceType.BpnRequestIdentifier
                        }
                    }
                ),
                addressName = postalAddress.addressName,
                identifiers = postalAddress.identifiers.map { org.eclipse.tractusx.orchestrator.api.model.Identifier(it.value, it.type, it.issuingBody) },
                states = postalAddress.states.map { org.eclipse.tractusx.orchestrator.api.model.BusinessState(it.validFrom, it.validTo, it.type) },
                confidenceCriteria = org.eclipse.tractusx.orchestrator.api.model.ConfidenceCriteria(
                    postalAddress.confidenceCriteria.sharedByOwner,
                    postalAddress.confidenceCriteria.checkedByExternalDataSource,
                    postalAddress.confidenceCriteria.numberOfSharingMembers,
                    postalAddress.confidenceCriteria.lastConfidenceCheckAt,
                    postalAddress.confidenceCriteria.nextConfidenceCheckAt,
                    postalAddress.confidenceCriteria.confidenceLevel
                ),
                physicalAddress = org.eclipse.tractusx.orchestrator.api.model.PhysicalAddress(
                    geographicCoordinates = org.eclipse.tractusx.orchestrator.api.model.GeoCoordinate(
                        postalAddress.physicalAddress.geographicCoordinates.longitude,
                        postalAddress.physicalAddress.geographicCoordinates.latitude,
                        postalAddress.physicalAddress.geographicCoordinates.altitude
                    ),
                    country = postalAddress.physicalAddress.country,
                    administrativeAreaLevel1 = postalAddress.physicalAddress.administrativeAreaLevel1,
                    administrativeAreaLevel2 = postalAddress.physicalAddress.administrativeAreaLevel2,
                    administrativeAreaLevel3 = postalAddress.physicalAddress.administrativeAreaLevel3,
                    postalCode = postalAddress.physicalAddress.postalCode,
                    city = postalAddress.physicalAddress.city,
                    district = postalAddress.physicalAddress.district,
                    street = org.eclipse.tractusx.orchestrator.api.model.Street(
                        postalAddress.physicalAddress.street.name,
                        postalAddress.physicalAddress.street.houseNumber,
                        postalAddress.physicalAddress.street.houseNumberSupplement,
                        postalAddress.physicalAddress.street.milestone,
                        postalAddress.physicalAddress.street.direction,
                        postalAddress.physicalAddress.street.namePrefix,
                        postalAddress.physicalAddress.street.additionalNamePrefix,
                        postalAddress.physicalAddress.street.nameSuffix,
                        postalAddress.physicalAddress.street.additionalNameSuffix
                    ),
                    companyPostalCode = postalAddress.physicalAddress.companyPostalCode,
                    industrialZone = postalAddress.physicalAddress.industrialZone,
                    building = postalAddress.physicalAddress.building,
                    floor = postalAddress.physicalAddress.floor,
                    door = postalAddress.physicalAddress.door,
                    taxJurisdictionCode = postalAddress.physicalAddress.taxJurisdictionCode
                ),
                alternativeAddress = postalAddress.alternativeAddress?.let {
                    org.eclipse.tractusx.orchestrator.api.model.AlternativeAddress(
                        geographicCoordinates = org.eclipse.tractusx.orchestrator.api.model.GeoCoordinate(
                            it.geographicCoordinates.longitude,
                            it.geographicCoordinates.latitude,
                            it.geographicCoordinates.altitude
                        ),
                        country = it.country,
                        administrativeAreaLevel1 = it.administrativeAreaLevel1,
                        postalCode = it.postalCode,
                        city = it.city,
                        deliveryServiceType = it.deliveryServiceType,
                        deliveryServiceQualifier = it.deliveryServiceQualifier,
                        deliveryServiceNumber = it.deliveryServiceNumber
                    )
                },
                hasChanged = postalAddress.hasChanged,
                goldenRecordRelations = emptyList(),
                updatedAt = null
            )
        )
}
