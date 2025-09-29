import type { LinkedDataFormats, PropertyTableEntryNode } from '@piveau/sdk-vue';
import type { Dataset } from '../../../model/dataset'
import { DcatApChV2DistributionAdapter } from './dcat-ap-ch-v2-distribution-adapter'
import type { TableEntry } from './table-entry';
import type { Catalog } from '~/piveau/get-ods-catalog-info';

export class DcatApChV2DatasetAdapter {
  #dataset: Dataset;

  constructor(d: Dataset) {
    this.#dataset = d;
  }

  /**
   * Get the id of the dataset
   *
   */
  get id() {
    return this.#dataset.getId;
  }

  /**
   * Get the title of the dataset. The title is mandatory but if not available it returns the id.
   * The language handling is done by piveau.
   *
   * from dcat-ap-ch:
   * Property	Title
   * Requirement level	Mandatory
   * Cardinality	1..n
   * URI	dct:title
   * Range	rdfs:Literal
   * Usage Note
   * This property contains a name given to the Dataset.
   * This property can be repeated for parallel language versions of the title (see 2.3 Multilingualism).
   */
  get title(): string {
    return this.#dataset?.getTitle ?? this.id;
  }

  /**
   * Get the description of the dataset. A description is mandatory but if not available it returns an empty string.
   *
   * The language handling is done by piveau.
   *
   * from dcat-ap-ch:
   * Property	description
   * Requirement level	Mandatory
   * Cardinality	1..n
   * URI	dct:description
   * Range	rdfs:Literal
   * Usage Note
   * This property contains a free-text account of the Dataset.
   * This property can be repeated for parallel language versions of the description (see 2.3 Multilingualism). On the user interface of data portals, the content of the element whose language corresponds to the display language selected by the user is displayed.
   */
  get description(): string | undefined {
    return (this.#dataset?.getDescription ?? '').replaceAll(/\r\n/g, '\n').trim();
  }

  get getCategories(): { label: Record<string, string>; id: string; resource: string; }[] {
    return this.#dataset?.getCategories ?? [];
  }

  /**
   * Get the publisher of the dataset.
   *
   * from dcat-ap-ch:
   * Property	publisher
   * Requirement level	Mandatory
   * Cardinality	1..1
   * URI	dct:publisher
   * Range	foaf:Agent
   * Usage Note
   * - This property refers to an entity (organisation) responsible for making the Dataset available.
   *
   */
  get publisher() {
    const publisher = this.#dataset.getPublisher;
    // the interface in piveau is wrong.
    // i get this ...
    // {
    //   name: "Bundesamt für Lebensmittelsicherheit und Veterinärwesen BLV",
    //   resource: "https://www.blv.admin.ch",
    //   type: "Agent",
    // }

    if (!publisher) {
      return undefined
    }

    return {
      type: publisher.type ?? '',
      name: publisher.name ?? '',
      resource: (publisher as unknown as { resource: string }).resource ?? ''
    }

  }

  get getLicenses(): string[] {
    return this.#dataset?.getLicenses ?? [];
  }
  /**
   * Get the licenses of the dataset.
   *
   */
  get licenses(): string[] {
    return this.#dataset?.getLicenses ?? [];
  }

  /**
   * Get the release date of the dataset if available
   *
   * from dcat-ap-ch:
   * Property	release date
   * Requirement level	Recommended
   * Cardinality	0..1
   * URI	dct:issued
   * Range	rdfs:Literal (typed as as xsd:date, xsd:dateTime, xsd:gYear or xsd:gYearMonth)
   * Usage Note
   * - This property contains the date of formal issuance (e.g., first publication of the Dataset).
   * - If this date is not known, the date of the first referencing of the data collection in the Catalogue can be entered.
   */
  get releaseDate() {
    if (!this.#dataset.getIssued) {
      return undefined
    }
    return new Date(this.#dataset.getIssued)
  }

  /**
   * Get the modification date of the dataset if available
   *
   * from dcat-ap-ch:
   * Property	update/ modification date
   * Requirement level	Recommended
   * Cardinality	0..1
   * URI	dct:modified
   * Range	rdfs:Literal (typed as as xsd:date, xsd:dateTime, xsd:gYear or xsd:gYearMonth)
   * Usage Note
   * - This property contains the most recent date on which the Dataset was changed or modified.
   * - No value may indicate that the Dataset has never changed after its initial publication,
   *   or that the date of the last modification is not known, or that the Dataset is continuously updated
   * - This property MUST only be set if the distributions (the actual data) that the Dataset describes
   *   have been updated after it has been issued. In this case the property MUST contain the date of the last update.
   *   That way a person or institution using the data for an analysis or application will know when to update the
   *   report or application on their side.
   */
  get modificationDate() {
    if (!this.#dataset.getModified) {
      return undefined
    }
    return new Date(this.#dataset.getModified)
  }

  get getLinkedData(): Record<LinkedDataFormats, string> {
    return this.#dataset?.getLinkedData ?? {} as Record<LinkedDataFormats, string>;
  }

  get getDistributions(): { id: string; title: string | undefined; description: string | undefined; format: string; modified: string | undefined; license: { label?: string | null | undefined; id?: string | null | undefined; description?: string | null | undefined; resource?: string | null | undefined; } | undefined; created: string | undefined; issued: string | undefined; languages: string[]; accessUrls: string[]; downloadUrls: string[]; getLinkedData: Record<LinkedDataFormats, string>; getPropertyTable: PropertyTableEntryNode[]; }[] {
    return this.#dataset?.getDistributions ?? [];
  }

  /**
   * Returns the distributions wrapped in DistributionAdapter instances
   *
   * @returns {DistributionAdapter[]} An array of DistributionAdapter instances
   */
  get distributions() {
    return this.#dataset.getDistributions.map(d => new DcatApChV2DistributionAdapter(d, this));
  }

  /**
   * Get the available formats of the dataset
   */
  get getOdsFormats(): { id?: string | null | undefined; label?: string | null | undefined; resource?: string | null | undefined; }[] {
    return this.#dataset?.getOdsFormats ?? [];
  }


  get getKeywords(): { id: string; label: string; language: string; }[] {
    return this.#dataset?.getKeywords ?? [];
  }

  /**
   * Get the catalog information of the dataset if available
   */
  get catalog(): Catalog {
    return this.#dataset.getOdsCatalogInfo
  }


  /**
   * Get the accrual periodicity of the dataset if available
   *
   * from dcat-ap-ch:
   * Property	frequency
   * Requirement level	Optional
   * Cardinality	0..1
   * URI	dct:accrualPeriodicity
   * Range	dct:Frequency
   * Usage Note
   * - This property refers to the frequency at which the Dataset is updated.
   * - CV to be used: [VOCAB-EU-FREQUENCY].
   */
  get frequency() {
    return this.#dataset.getOdsAccrualPeriodicity;
  }

  get propertyTable() {
    const rootNode = this.#dataset.getPropertyTable;
    if (!rootNode) {
      return [];
    }

    const ignoredNode = ['catalogRecord'];
    const nodesToConsider = rootNode.filter(n => n.data).filter(n => !ignoredNode.includes(n.id));

    const table: TableEntry[] = [];
    for (const node of nodesToConsider) {

      const entry = {} as Partial<TableEntry>;

      if (node.type === 'node' && node.data) {
        entry.label = node.label;
        entry.nodeType = 'node';

        if (node.data && node.data.length > 0) {
          for (const child of node.data) {
            if (child.type === 'value') {
              if (!entry.value) {
                entry.value = [{ value: child.data as string, type: 'value' }];
              } else {
                entry.value.push({ value: child.data as string, type: 'value' });
              }
            } else if (child.type === 'href') {
              const hrefData = child.data as { label: string; href: string };
              if (!entry.value) {
                entry.value = [{ value: hrefData.label, href: hrefData.href, type: 'href' }];
              } else {
                entry.value.push({ value: hrefData.label, href: hrefData.href, type: 'href' });
              }
            } else {
              if (node.id === 'publisher') {
                // special handling for publisher node
                for (const data of child.data ?? []) {
                  if (!entry.value) {
                    entry.value = [{ value: data.data as string, type: 'value' }];
                  } else {
                    entry.value.push({ value: data.data as string, type: 'value' });
                  }
                }
              } else if (node.id === 'contactPoint') {
                // special handling for contactPoint node
                const nameNode = child.data?.find(d => d.id === 'contactPointName')
                const emailNode = child.data?.find(d => d.id === 'contactPointEmail')
                if (nameNode && emailNode && nameNode.data && emailNode.data) {

                  // eslint-disable-next-line @typescript-eslint/no-explicit-any
                  const emailData = ((emailNode.data as Array<any>)[0] as any).data;
                  const href = emailData.href;
                  const name = emailData.label;

                  // eslint-disable-next-line @typescript-eslint/no-explicit-any
                  const nameData = ((nameNode.data as Array<any>)[0] as any).data;
                  const publisherName = nameData as string;
                  if (!entry.value) {
                    entry.value = [{ value: publisherName, type: 'value' }];
                    entry.value.push({ value: name, href, type: 'email' });
                  }
                }
              }
            }
          }
          table.push(entry as TableEntry);
        }

      }

    }
    return table.sort((a, b) => a.label.localeCompare(b.label));
  }
}



