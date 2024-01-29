# Real World App

## The mother of all demo apps

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://github.com/raahii/golang-grpc-realworld-example/blob/master/LICENSE)

> ### Clojure/Hiccup/HTMX/XTDB codebase containing RealWorld examples (CRUD, auth, advanced patterns, etc) that adheres to the [RealWorld](https://github.com/gothinkster/realworld) spec as much as possible while sticking to HATEOAS principals.

### [Demo](https://github.com/gothinkster/realworld)&nbsp;&nbsp;&nbsp;&nbsp;[RealWorld](https://github.com/gothinkster/realworld)

This codebase was created to demonstrate a fully fledged backend application built with Clojure/HTMX including CRUD operations, authentication, routing, pagination, and more.

## How it works

- Using **Clojure** to implement realworld backend server.

  - Ring: [ring](https://github.com/ring-clojure/ring)
  - Reitit: [reitit]:(https://github.com/metosin/reitit)
  - Hiccup: [hiccup]:(https://github.com/weavejester/hiccup)
  - HTMX: [htmx](https://htmx.org/)
  - Hyperscript: [hyperscript](https://hyperscript.org/)

- Using **XTDB** to store data.
- Using **Nix Flakes** to build and deploy

## Getting started

The app listens and serves on `0.0.0.0:3000`.

Do drop into a nix shell (you will need the nix package manager installed)

```bash
nix develop
```

This will install the binaries needed as well as display useful information to get started developing.

## Project status

In Progress...

### Todo

- Add seed data import
  - [ ] Add xtdb
  - [ ] Add init script
  - [ ] Add comments
  - [ ] Add users
  - [ ] Add articles
  - [ ] Add get route
  - [ ] Add author details
  - [ ] Add default user on cli flag
- Home
  - [x] Render Page
  - [ ] Load tags
- Articles Api
  - [ ] Filter articles
    - [ ] by author
    - [ ] Add tag
    - [ ] add favorites
  - [ ] Unfavorite
  - [ ] Insert tags
  - [ ] Get popular tags
- Views
  - [x] add banners
  - [x] add error message on htmx 500
  - [ ] add flash on oob request
- Comments
  - API
    - [ ] create comment on article
    - [ ] delete comment from article
    - [ ] get articles by slug
  - Repo
    - [ ] create
    - [ ] read
      - [ ] by id
      - [ ] by author
      - [ ] by article
    - [ ] delete
  - Service
    - [ ] create
    - [ ] read
    - [ ] delete
    - [ ] mark if owner
- Get Article page
  - [ ] delete if owner
  - [ ] Follow/Unfollow author
  - [ ] Favorite/Unfavorite article
  - [ ] comments
    - [ ] Get on load
    - [ ] mark if owner
  - [ ] delete comment if owner
  - [ ] /article/:slug
- [ ] Docker Build
  > a build to let non-nix users run server
- Get new/edit articles page
  - [ ] edit tags
  - [ ] /editor/slug - edit
    - [ ] Load article and prefill page
    - [ ] update links to patch
  - [ ] /editor - new

#### Doing

- Profile page
  - [x] Get profile page
  - [x] link to settings
  - [x] hide follow if own profile
  - [ ] Make content live
  - [ ] follow author
  - [ ] show favorited articles

#### Done

- [x] refactor to xtdb
  - [x] swap out datalevin for xtdb
  - [x] swap session to xtdb or some kv store other then datalevin
- Auth
  - [x] Add is authed middleware
  - [x] Return and list errors from form
  - [ ] Login
    - [x] Post to Login
    - [x] Add user to session
    - [x] Add user data to layout
    - [x] Flash message on success
  - [x] Sign Up
    - [x] Post
    - [x] Create user
    - [x] Flash message on success
    - [x] Sign out
    - [x] Can re-login
- Settings page
  - [x] Get settings page
  - [x] make errors dynamic
  - [x] add post for form
  - [x] add logout handler
  - [x] Update settings
    - [x] image
    - [x] username
    - [x] bio
    - [x] email
    - [x] password
