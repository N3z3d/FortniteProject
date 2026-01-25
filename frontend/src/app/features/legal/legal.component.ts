import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { TranslationService } from '../../core/services/translation.service';

type LegalPageType = 'contact' | 'legal-notice' | 'privacy';

interface PageContent {
  icon: string;
  titleKey: string;
  sections: { titleKey: string; contentKey: string }[];
}

@Component({
  selector: 'app-legal',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule
  ],
  templateUrl: './legal.component.html',
  styleUrls: ['./legal.component.scss']
})
export class LegalComponent implements OnInit {
  pageType: LegalPageType = 'legal-notice';
  readonly lastUpdateDate = '2026-01-22';

  private pageConfigs: Record<LegalPageType, PageContent> = {
    'contact': {
      icon: 'mail',
      titleKey: 'legal.contact.title',
      sections: [
        { titleKey: 'legal.contact.emailTitle', contentKey: 'legal.contact.emailContent' },
        { titleKey: 'legal.contact.supportTitle', contentKey: 'legal.contact.supportContent' }
      ]
    },
    'legal-notice': {
      icon: 'gavel',
      titleKey: 'legal.legalNotice.title',
      sections: [
        { titleKey: 'legal.legalNotice.editorTitle', contentKey: 'legal.legalNotice.editorContent' },
        { titleKey: 'legal.legalNotice.hostingTitle', contentKey: 'legal.legalNotice.hostingContent' },
        { titleKey: 'legal.legalNotice.intellectualTitle', contentKey: 'legal.legalNotice.intellectualContent' }
      ]
    },
    'privacy': {
      icon: 'security',
      titleKey: 'legal.privacy.title',
      sections: [
        { titleKey: 'legal.privacy.dataTitle', contentKey: 'legal.privacy.dataContent' },
        { titleKey: 'legal.privacy.cookiesTitle', contentKey: 'legal.privacy.cookiesContent' },
        { titleKey: 'legal.privacy.rightsTitle', contentKey: 'legal.privacy.rightsContent' }
      ]
    }
  };

  constructor(
    private route: ActivatedRoute,
    public t: TranslationService
  ) {}

  ngOnInit(): void {
    this.route.data.subscribe(data => {
      this.pageType = data['pageType'] || 'legal-notice';
    });
  }

  get config(): PageContent {
    return this.pageConfigs[this.pageType];
  }

  getTitle(): string {
    return this.t.t(this.config.titleKey, this.getDefaultTitle());
  }

  private getDefaultTitle(): string {
    switch (this.pageType) {
      case 'contact': return 'Contact';
      case 'legal-notice': return 'Mentions Legales';
      case 'privacy': return 'Politique de Confidentialite';
    }
  }
}
