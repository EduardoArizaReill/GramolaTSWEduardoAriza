export class PlayList {
  collaborative: boolean;
  description: string;
  external_urls: ExternalUrls;
  href: string;
  id: string;
  images: Image[];
  name: string;
  owner: Owner;
  public: boolean;
  snapshot_id: string;
  tracks: Tracks;
  type: string;
  uri: string;

  constructor(
    collaborative: boolean,
    description: string,
    external_urls: ExternalUrls,
    href: string,
    id: string,
    images: Image[],
    name: string,
    owner: Owner,
    isPublic: boolean,
    snapshot_id: string,
    tracks: Tracks,
    type: string,
    uri: string
  ) {
    this.collaborative = collaborative;
    this.description = description;
    this.external_urls = external_urls;
    this.href = href;
    this.id = id;
    this.images = images;
    this.name = name;
    this.owner = owner;
    this.public = isPublic;
    this.snapshot_id = snapshot_id;
    this.tracks = tracks;
    this.type = type;
    this.uri = uri;
  }

  static fromJSON(data: any): PlayList {
    return new PlayList(
      data.collaborative,
      data.description,
      ExternalUrls.fromJSON(data.external_urls),
      data.href,
      data.id,
      (data.images || []).map((img: any) => Image.fromJSON(img)),
      data.name,
      Owner.fromJSON(data.owner),
      data.public,
      data.snapshot_id,
      Tracks.fromJSON(data.tracks),
      data.type,
      data.uri
    );
  }
}

export class ExternalUrls {
  spotify: string;

  constructor(spotify: string) {
    this.spotify = spotify;
  }

  static fromJSON(data: any): ExternalUrls {
    return new ExternalUrls(data.spotify);
  }
}

export class Image {
  url: string;
  height: number;
  width: number;

  constructor(url: string, height: number, width: number) {
    this.url = url;
    this.height = height;
    this.width = width;
  }

  static fromJSON(data: any): Image {
    return new Image(data.url, data.height, data.width);
  }
}

export class Owner {
  external_urls: ExternalUrls;
  href: string;
  id: string;
  type: string;
  uri: string;
  display_name: string;

  constructor(
    external_urls: ExternalUrls,
    href: string,
    id: string,
    type: string,
    uri: string,
    display_name: string
  ) {
    this.external_urls = external_urls;
    this.href = href;
    this.id = id;
    this.type = type;
    this.uri = uri;
    this.display_name = display_name;
  }

  static fromJSON(data: any): Owner {
    return new Owner(
      ExternalUrls.fromJSON(data.external_urls),
      data.href,
      data.id,
      data.type,
      data.uri,
      data.display_name
    );
  }
}

export class Tracks {
  href: string;
  total: number;

  constructor(href: string, total: number) {
    this.href = href;
    this.total = total;
  }

  static fromJSON(data: any): Tracks {
    return new Tracks(data.href, data.total);
  }
}
