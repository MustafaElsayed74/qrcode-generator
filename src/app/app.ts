import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpClientModule } from '@angular/common/http';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule, HttpClientModule],
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class App {
  protected readonly title = signal('QR Code Generator');

  // Backend API base URL - works for both local and ngrok deployment
  private apiBaseUrl = this.getApiBaseUrl();

  // form model
  type = 'text';
  text = '';
  url = '';
  facebook = '';
  twitter = '';
  instagram = '';
  linkedin = '';
  imageUrl = '';
  size = 300;
  theme = 'classic';
  fgColor = '#000000';
  bgColor = '#ffffff';

  // social platform selection checkboxes
  socialPlatforms = {
    facebook: false,
    twitter: false,
    instagram: false,
    linkedin: false
  };

  selectedFile: File | null = null;
  previewUrl: string | null = null;
  downloadUrl: string | null = null;

  constructor(private http: HttpClient) {
    console.log('Using API base URL:', this.apiBaseUrl);
  }

  // Detect if backend is accessible and return appropriate URL
  private getApiBaseUrl(): string {
    const origin = window.location.origin;
    const host = window.location.hostname;
    const port = window.location.port;

    // Use same origin unless running Angular dev server on 4200
    if (!(host === 'localhost' && port === '4200')) {
      return origin;
    }

    // Fallback to local backend for development (Angular dev server on 4200)
    return 'http://localhost:9091';
  }

  onFileChange(e: Event) {
    const input = e.target as HTMLInputElement;
    if (input.files && input.files.length) {
      this.selectedFile = input.files[0];
    }
  }

  private setPreviewFromBlob(blob: Blob) {
    if (this.previewUrl) URL.revokeObjectURL(this.previewUrl);
    const url = URL.createObjectURL(blob);
    this.previewUrl = url;
    this.downloadUrl = url;
  }

  async uploadFile(file: File) {
    const fd = new FormData();
    fd.append('file', file);
    try {
      const res = await this.http.post(`${this.apiBaseUrl}/api/upload-image`, fd).toPromise() as any;
      return res?.url;
    } catch (err) {
      console.error('Upload failed:', err);
      throw err;
    }
  }

  async generate() {
    try {
      if (!this.text && !this.url && !this.facebook && !this.twitter && !this.instagram && !this.linkedin && !this.imageUrl && !this.selectedFile) {
        alert('Please enter some content first');
        return;
      }

      const size = this.size || 300;
      if (this.type === 'text') {
        const q = encodeURIComponent(this.text || '');
        console.log('Generating text QR:', q);
        let url = `${this.apiBaseUrl}/api/qr?text=${q}&size=${size}`;
        if (this.theme && this.theme !== 'custom') {
          url += `&theme=${encodeURIComponent(this.theme)}`;
        } else {
          url += `&fg=${encodeURIComponent(this.fgColor)}&bg=${encodeURIComponent(this.bgColor)}`;
        }
        const blob = await this.http.get(url, { responseType: 'blob' }).toPromise();
        this.setPreviewFromBlob(blob as Blob);
      } else if (this.type === 'url') {
        const q = encodeURIComponent(this.url || '');
        console.log('Generating URL QR:', q);
        let url = `${this.apiBaseUrl}/api/qr?text=${q}&size=${size}`;
        if (this.theme && this.theme !== 'custom') {
          url += `&theme=${encodeURIComponent(this.theme)}`;
        } else {
          url += `&fg=${encodeURIComponent(this.fgColor)}&bg=${encodeURIComponent(this.bgColor)}`;
        }
        const blob = await this.http.get(url, { responseType: 'blob' }).toPromise();
        this.setPreviewFromBlob(blob as Blob);
      } else if (this.type === 'social') {
        const selectedPlatforms: { [key: string]: string } = {};
        if (this.socialPlatforms['facebook'] && this.facebook) selectedPlatforms['facebook'] = this.facebook;
        if (this.socialPlatforms['twitter'] && this.twitter) selectedPlatforms['twitter'] = this.twitter;
        if (this.socialPlatforms['instagram'] && this.instagram) selectedPlatforms['instagram'] = this.instagram;
        if (this.socialPlatforms['linkedin'] && this.linkedin) selectedPlatforms['linkedin'] = this.linkedin;

        if (Object.keys(selectedPlatforms).length === 0) {
          alert('Please select at least one social platform and enter a URL');
          return;
        }

        const payload: any = {
          type: 'social',
          size,
          payload: selectedPlatforms
        };
        if (this.theme && this.theme !== 'custom') {
          payload.theme = this.theme;
        } else {
          payload.theme = 'custom';
          payload.fgColor = this.fgColor;
          payload.bgColor = this.bgColor;
        }
        console.log('Generating social QR:', payload);
        const blob = await this.http.post(`${this.apiBaseUrl}/api/qr`, payload, { responseType: 'blob' }).toPromise();
        this.setPreviewFromBlob(blob as Blob);
      } else if (this.type === 'imageUrl') {
        let target = this.imageUrl;
        if (!target && this.selectedFile) {
          console.log('Uploading file:', this.selectedFile.name);
          const url = await this.uploadFile(this.selectedFile);
          if (!url) { alert('Upload failed'); return; }
          target = url;
        }

        if (!target) {
          alert('Please enter an image URL or upload a file');
          return;
        }

        const payload: any = {
          type: 'imageUrl',
          size,
          imageUrl: target
        };
        if (this.theme && this.theme !== 'custom') {
          payload.theme = this.theme;
        } else {
          payload.theme = 'custom';
          payload.fgColor = this.fgColor;
          payload.bgColor = this.bgColor;
        }
        console.log('Generating image QR:', payload);
        const blob = await this.http.post(`${this.apiBaseUrl}/api/qr`, payload, { responseType: 'blob' }).toPromise();
        this.setPreviewFromBlob(blob as Blob);
      }
    } catch (err) {
      console.error('QR generation failed:', err);
      alert(`Failed to generate QR code.\n\nMake sure backend is running at: ${this.apiBaseUrl}\n\nCheck browser console (F12) for details.`);
    }
  }
}

